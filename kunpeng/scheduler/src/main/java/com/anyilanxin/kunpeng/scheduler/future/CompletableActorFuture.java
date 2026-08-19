/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.scheduler.future;

import com.anyilanxin.kunpeng.scheduler.ActorControl;
import com.anyilanxin.kunpeng.scheduler.Loggers;
import com.anyilanxin.kunpeng.scheduler.core.ActorCell;
import com.anyilanxin.kunpeng.scheduler.core.CarrierContext;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.slf4j.Logger;

/**
 * 可完成 future：VarHandle 单次完成状态机；actor 线程未完成 get() 抛 {@link IllegalStateException}（非阻塞纪律）；close()
 * 后可复用。
 */
@SuppressWarnings("NullableProblems")
public class CompletableActorFuture<V> implements ActorFuture<V> {

  private static final Logger LOG = Loggers.FUTURE_LOGGER;
  private static final VarHandle STATE;

  static {
    try {
      STATE =
          MethodHandles.lookup().findVarHandle(CompletableActorFuture.class, "state", int.class);
    } catch (final ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static final int AWAITING_RESULT = 1;
  private static final int COMPLETING = 2;
  private static final int COMPLETED = 3;
  private static final int COMPLETED_EXCEPTIONALLY = 4;
  private static final int CLOSED = 5;

  @SuppressWarnings("unused")
  private volatile int state = AWAITING_RESULT;

  private V value;
  private String failure;
  private Throwable failureCause;
  private long completedAt = -1;

  // 注册/排空互斥: 封死 "offer 进队 vs 完成排空已结束" 的可见性竞态
  private final Object completionSync = new Object();

  // 惰性创建的阻塞等待设施（多数 future 不会被外部线程等待）
  private ReentrantLock completionLock;
  private Condition isDoneCondition;
  private final ManyToOneConcurrentLinkedQueue<BiConsumer<V, Throwable>> blockedCallbacks =
      new ManyToOneConcurrentLinkedQueue<>();

  public CompletableActorFuture() {}

  public static <V> CompletableActorFuture<V> completed() {
    return completed(null);
  }

  public static <V> CompletableActorFuture<V> completed(final V value) {
    final CompletableActorFuture<V> future = new CompletableActorFuture<>();
    future.complete(value);
    return future;
  }

  public static <V> CompletableActorFuture<V> completedExceptionally(final Throwable throwable) {
    final CompletableActorFuture<V> future = new CompletableActorFuture<>();
    future.completeExceptionally(throwable);
    return future;
  }

  /** 复用复位（回到 AWAITING） */
  public void setAwaitingResult() {
    state = AWAITING_RESULT;
    value = null;
    failure = null;
    failureCause = null;
    completedAt = -1;
  }

  public boolean isAwaitingResult() {
    return (int) STATE.getAcquire(this) == AWAITING_RESULT;
  }

  @Override
  public void complete(final V value) {
    if (STATE.compareAndSet(this, AWAITING_RESULT, COMPLETING)) {
      this.value = value;
      completedAt = System.currentTimeMillis();
      STATE.setRelease(this, COMPLETED);
      onCompleted();
    } else {
      throw new IllegalStateException("Future is already completed, cannot complete again");
    }
  }

  @Override
  public void completeExceptionally(final String failure, final Throwable throwable) {
    if (STATE.compareAndSet(this, AWAITING_RESULT, COMPLETING)) {
      this.failure = failure;
      this.failureCause = throwable;
      completedAt = System.currentTimeMillis();
      STATE.setRelease(this, COMPLETED_EXCEPTIONALLY);
      onCompleted();
    } else {
      throw new IllegalStateException("Future is already completed, cannot complete again");
    }
  }

  @Override
  public void completeExceptionally(final Throwable throwable) {
    completeExceptionally(throwable.getMessage(), throwable);
  }

  /** 用另一 future 的结果完成自己 */
  public void completeWith(final ActorFuture<V> otherFuture) {
    if (otherFuture.isCompletedExceptionally()) {
      completeExceptionally(otherFuture.getException());
    } else {
      try {
        complete(otherFuture.get());
      } catch (final Exception e) {
        completeExceptionally(e);
      }
    }
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    final int currentState = (int) STATE.getAcquire(this);
    return currentState == COMPLETED || currentState == COMPLETED_EXCEPTIONALLY;
  }

  @Override
  public boolean isCompletedExceptionally() {
    return (int) STATE.getAcquire(this) == COMPLETED_EXCEPTIONALLY;
  }

  @Override
  public Throwable getException() {
    if (!isCompletedExceptionally()) {
      throw new IllegalStateException("Future is not completed exceptionally");
    }
    return failureCause != null ? failureCause : new RuntimeException(failure);
  }

  public String getFailure() {
    return failure;
  }

  public long getCompletedAt() {
    return completedAt;
  }

  @Override
  public V get() throws ExecutionException, InterruptedException {
    try {
      return get(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
    } catch (final TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public V get(final long timeout, final TimeUnit unit)
      throws ExecutionException, TimeoutException, InterruptedException {
    if (CarrierContext.current() != null) {
      if (!isDone()) {
        LOG.error(
            "Actor call get() on future which has not completed.  Actors must be non-blocking. Use actor.runOnCompletion().");
        throw new IllegalStateException(
            "Actor call get() on future which has not completed. "
                + "Actors must be non-blocking. Use actor.runOnCompletion().");
      }
    } else {
      // 非 actor 线程的阻塞等待
      final ReentrantLock lock = getCompletionLock();
      lock.lock();
      try {
        long remaining = unit.toNanos(timeout);
        while (!isDone()) {
          if (remaining <= 0) {
            LOG.error("Timeout after: {} {}", timeout, unit);
            throw new TimeoutException("Timeout after: " + timeout + " " + unit);
          }
          remaining = isDoneCondition().awaitNanos(remaining);
        }
      } finally {
        lock.unlock();
      }
    }

    if (isCompletedExceptionally()) {
      throw new ExecutionException(failure, failureCause);
    }
    return value;
  }

  @Override
  public V join() {
    return FutureUtil.join(this);
  }

  @Override
  public V join(final long timeout, final TimeUnit timeUnit) {
    return FutureUtil.join(this, timeout, timeUnit);
  }

  @Override
  public void block(final ActorCell onCompletion) {
    // 已完成的 future 回调永不再触发——必须立即唤醒, 否则 cell 永眠
    // （启动链 runOnCompletion(completed future) 大量出现此形态）
    synchronized (completionSync) {
      if (!isDone()) {
        blockedCallbacks.add((resIgnore, errorIgnore) -> onCompletion.wakeSignal());
        return;
      }
    }
    onCompletion.wakeSignal();
  }

  @Override
  public void onComplete(final BiConsumer<V, Throwable> consumer) {
    if (CarrierContext.onActorThread()) {
      final ActorControl actorControl = ActorControl.current();
      actorControl.runOnCompletion(this, consumer);
    } else {
      // 测试可用; 生产代码应带 executor
      LOG.warn(
          "[PotentiallyBlocking] No executor provided for ActorFuture#onComplete callback."
              + " This could block the actor that completes the future."
              + " Use onComplete(consumer, executor) instead.");
      onComplete(consumer, Runnable::run);
    }
  }

  @Override
  public void onComplete(final BiConsumer<V, Throwable> consumer, final Executor executor) {
    // 注册前/后完成竞态: executedOnce 保证 exactly-once
    final AtomicBoolean executedOnce = new AtomicBoolean(false);
    final BiConsumer<V, Throwable> checkedConsumer =
        (res, error) ->
            executor.execute(
                () -> {
                  if (executedOnce.compareAndSet(false, true)) {
                    consumer.accept(res, error);
                  }
                });

    synchronized (completionSync) {
      if (!isDone()) {
        blockedCallbacks.add(checkedConsumer);
      }
    }
    // monitor happens-before 保证: 若未入队则此刻必能读到完成态
    if (isDone()) {
      checkedConsumer.accept(value, failureCause);
    }
  }

  @Override
  public <U> ActorFuture<U> andThen(final Supplier<ActorFuture<U>> next, final Executor executor) {
    return andThen(
        (v, e) -> {
          if (e != null) {
            return CompletableActorFuture.<U>completedExceptionally(e);
          }
          return next.get();
        },
        executor);
  }

  /** 与旧语义对齐：BiFunction 形态接收上游错误（可自行恢复）；Supplier/Function 形态对上游错误 快速失败。next 返回 null 视为完成 null。 */
  @Override
  public <U> ActorFuture<U> andThen(
      final BiFunction<V, Throwable, ActorFuture<U>> next, final Executor executor) {
    final CompletableActorFuture<U> result = new CompletableActorFuture<>();
    onComplete(
        (v, e) ->
            executor.execute(
                () -> {
                  try {
                    final ActorFuture<U> nextFuture = next.apply(v, e);
                    if (nextFuture == null) {
                      result.complete(null);
                      return;
                    }
                    // result 的完成不能经 executor 路由: next 的典型形态是 actor 自身的
                    // closeFuture, 其完成发生在 runOnActorClosedAndCleanup 内(actor 已终态),
                    // executor(=该 actor) 会静默拒绝 job → 完成丢失 → 外层链永挂。
                    // CompletableActorFuture 完成本身线程安全, 内联完成即可。
                    nextFuture.onComplete(
                        (nv, ne) -> {
                          if (ne != null) {
                            result.completeExceptionally(ne);
                          } else {
                            result.complete(nv);
                          }
                        },
                        Runnable::run);
                  } catch (final Throwable t) {
                    result.completeExceptionally(t);
                  }
                }),
        Runnable::run);
    return result;
  }

  @Override
  public <U> ActorFuture<U> andThen(
      final Function<V, ActorFuture<U>> next, final Executor executor) {
    return andThen(
        (v, e) -> {
          if (e != null) {
            return CompletableActorFuture.completedExceptionally(e);
          }
          return next.apply(v);
        },
        executor);
  }

  @Override
  public <U> ActorFuture<U> thenApply(final Function<V, U> next, final Executor executor) {
    final CompletableActorFuture<U> result = new CompletableActorFuture<>();
    onComplete(
        (v, e) ->
            executor.execute(
                () -> {
                  if (e != null) {
                    result.completeExceptionally(e);
                  } else {
                    try {
                      result.complete(next.apply(v));
                    } catch (final Throwable t) {
                      result.completeExceptionally(t);
                    }
                  }
                }),
        Runnable::run);
    return result;
  }

  @Override
  public <U> ActorFuture<U> thenApply(final Function<V, U> next) {
    return thenApply(next, Runnable::run);
  }

  /** 复用关闭：回调清空、状态复位 */
  public void close() {
    if (isDone()) {
      blockedCallbacks.clear();
      setAwaitingResult();
    } else {
      STATE.setRelease(this, CLOSED);
    }
  }

  // ===== 内部 =====

  private void onCompleted() {
    signalWaiters();
    synchronized (completionSync) {
      drainCallbacks();
    }
  }

  private void drainCallbacks() {
    BiConsumer<V, Throwable> callback;
    while ((callback = blockedCallbacks.poll()) != null) {
      try {
        callback.accept(value, failureCause);
      } catch (final RuntimeException e) {
        LOG.error("Continuing on future completion failed", e);
      }
    }
  }

  private void signalWaiters() {
    if (completionLock != null) {
      completionLock.lock();
      try {
        if (isDoneCondition != null) {
          isDoneCondition.signalAll();
        }
      } finally {
        completionLock.unlock();
      }
    }
  }

  private ReentrantLock getCompletionLock() {
    if (completionLock == null) {
      synchronized (this) {
        if (completionLock == null) {
          completionLock = new ReentrantLock();
          isDoneCondition = completionLock.newCondition();
        }
      }
    }
    return completionLock;
  }

  private Condition isDoneCondition() {
    return isDoneCondition;
  }
}
