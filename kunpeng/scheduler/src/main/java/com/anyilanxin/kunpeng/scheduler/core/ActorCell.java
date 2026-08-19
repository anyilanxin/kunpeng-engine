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
package com.anyilanxin.kunpeng.scheduler.core;

import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorCondition;
import com.anyilanxin.kunpeng.scheduler.ActorControl;
import com.anyilanxin.kunpeng.scheduler.Loggers;
import com.anyilanxin.kunpeng.scheduler.ScheduledTimer;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.slf4j.Logger;

/**
 * 单 actor 执行单元（owner 线程独占执行）。
 *
 * <p>两级队列：fastLane（owner LIFO, actor 内自提交）+ submitted（外部 MPSC FIFO, 上限 10000 超限 fail）。调度门见 {@link
 * SchedulingGate}——同一 cell 在 runner 队列中至多 一个实例, 物理移除式偷取下天然单线程执行。
 *
 * <p>相位推进发生在排空点（drain 末尾）, 失败语义按相位分流（STARTED 存活 / 其他致死）。
 */
public final class ActorCell {

  private static final Logger LOG = Loggers.ACTOR_LOGGER;
  private static final int MAX_SUBMITTED_JOBS = 10_000;
  private static final int SELF_POOL_CAP = 256;

  private final Actor actor;
  private final SchedulingGate gate = new SchedulingGate();
  private final ArrayDeque<ActorEnvelope> fastLane = new ArrayDeque<>();
  private final ManyToOneConcurrentLinkedQueue<ActorEnvelope> submitted =
      new ManyToOneConcurrentLinkedQueue<>();
  private final AtomicInteger submittedCount = new AtomicInteger();
  // 唤醒票: 每个唤醒源在 tryWakeup 前自增; owner 以 drain 起点快照对比检测
  // "offer 对休眠中 owner 不可见" 的 JMM 竞态（volatile 全序封死）
  private final AtomicInteger wakeTickets = new AtomicInteger();
  private final List<SubscriptionSlot> subscriptions = new ArrayList<>();
  private final ArrayDeque<ActorEnvelope> selfPool = new ArrayDeque<>();

  private volatile Phases phase = Phases.CREATED;
  private volatile CellRunner homeRunner;
  private volatile CellPool pool;
  private SchedulerMetrics metrics;

  private ActorControl control;
  private CompletableActorFuture<Void> startingFuture;
  private CompletableActorFuture<Void> closeFuture;
  private boolean closeRequestedBeforeStarted;
  private boolean yieldRequested;

  public ActorCell(final Actor actor) {
    this.actor = actor;
  }

  // ===== 调度器侧 =====

  public void setControl(final ActorControl control) {
    this.control = control;
  }

  public ActorControl getControl() {
    return control;
  }

  public void onScheduled(final CellPool pool, final SchedulerMetrics metrics) {
    this.pool = pool;
    this.metrics = metrics;
    phase = Phases.STARTING;
    startingFuture = new CompletableActorFuture<>();
    offerInternal(ActorEnvelope.Kind.LIFECYCLE, actor::internalOnActorStarting, startingFuture);
  }

  public ActorFuture<Void> getStartingFuture() {
    return startingFuture;
  }

  public ActorFuture<Void> getCloseFuture() {
    return closeFuture;
  }

  /** 唤醒信号入口（任意线程）: 先记票再过门 */
  public void wakeSignal() {
    wakeTickets.incrementAndGet();
    tryWakeup();
  }

  /** 提交者侧唤醒入口（任意线程） */
  public void tryWakeup() {
    if (gate.tryWakeup() && pool != null) {
      pool.route(this);
    }
  }

  /** gate 内部路由（tryWakeup 成功后） */
  private void routeSelf() {
    if (pool != null) {
      pool.route(this);
    }
  }

  /** runner 取走 cell 后调用 */
  public void claimedBy(final CellRunner runner) {
    gate.markRunning();
    homeRunner = runner;
  }

  public SchedulingGate getGate() {
    return gate;
  }

  public Phases getPhase() {
    return phase;
  }

  public String getName() {
    return actor.getName();
  }

  public Map<String, String> getContext() {
    return actor.getContext();
  }

  public CellRunner getHomeRunner() {
    return homeRunner;
  }

  // ===== 提交路径 =====

  /** 外部提交（任意线程; submitActor 之前的早期提交合法——入队等待首次调度一并执行） */
  public <T> ActorFuture<T> submitExternal(
      final ActorEnvelope.Kind kind, final Runnable action, final Callable<T> callable) {
    final CompletableActorFuture<T> future = new CompletableActorFuture<>();
    if (pool == null) {
      LOG.warn(
          "Actor '{}' received a job before being submitted to the scheduler;"
              + " the job is queued and will run once submitActor() schedules the actor.",
          getName());
    }
    final Phases current = phase;
    if (current.isTerminal() || current == Phases.CLOSING || current == Phases.CLOSE_REQUESTED) {
      LOG.warn(
          "Job submitted to actor '{}' in phase {} was rejected; the job is dropped.",
          getName(),
          current);
      rejectJob(current == Phases.FAILED ? "ACTOR_FAILED" : "ACTOR_CLOSED");
      future.completeExceptionally(new IllegalStateException("Actor is closed"));
      return future;
    }
    final ActorEnvelope envelope = new ActorEnvelope();
    if (callable != null) {
      envelope.wrapCall(callable, future);
    } else {
      envelope.wrap(kind, action, future);
    }
    if (submittedCount.incrementAndGet() > MAX_SUBMITTED_JOBS) {
      submittedCount.decrementAndGet();
      rejectJob("QUEUE_FULL");
      future.completeExceptionally(
          new IllegalStateException("Submitted job queue is full for actor " + getName()));
      return future;
    }
    jobSubmitted();
    submitted.offer(envelope);
    wakeSignal();
    return future;
  }

  /** actor 内自提交（owner 线程; fast-lane 立即可见） */
  public <T> ActorFuture<T> submitInternal(
      final ActorEnvelope.Kind kind, final Runnable action, final Callable<T> callable) {
    final CompletableActorFuture<T> future = new CompletableActorFuture<>();
    final Phases current = phase;
    if (current.isTerminal()) {
      LOG.warn(
          "Self-submitted job to terminal actor '{}' (phase {}) was dropped.", getName(), current);
      future.completeExceptionally(new IllegalStateException("Actor is closed"));
      return future;
    }
    final ActorEnvelope envelope = takeSelf();
    if (callable != null) {
      envelope.wrapCall(callable, future);
    } else {
      envelope.wrap(kind, action, future);
    }
    jobSubmitted();
    fastLane.addLast(envelope);
    return future;
  }

  /** 无返回值提交（fire-and-forget） */
  public void submitVoid(final Runnable action) {
    if (pool == null) {
      LOG.warn(
          "Actor '{}' received a job before being submitted to the scheduler;"
              + " the job is queued and will run once submitActor() schedules the actor.",
          getName());
    }
    final Phases current = phase;
    if (current.isTerminal() || current == Phases.CLOSING || current == Phases.CLOSE_REQUESTED) {
      rejectJob(current == Phases.FAILED ? "ACTOR_FAILED" : "ACTOR_CLOSED");
      return;
    }
    final ActorEnvelope envelope = new ActorEnvelope();
    envelope.wrap(ActorEnvelope.Kind.RUN, action, null);
    if (submittedCount.incrementAndGet() > MAX_SUBMITTED_JOBS) {
      submittedCount.decrementAndGet();
      rejectJob("QUEUE_FULL");
      return;
    }
    jobSubmitted();
    submitted.offer(envelope);
    wakeSignal();
  }

  /** runUntilDone 的重复提交（owner） */
  public void repeatInternal(final ActorEnvelope envelope) {
    fastLane.addLast(envelope);
  }

  /** runBlocking 完成（任意线程 → 外部路径） */
  public void submitBlockedDone(final Runnable continuation) {
    submitVoid(continuation);
  }

  // ===== 订阅 =====

  public ActorCondition onCondition(final String conditionName, final Runnable action) {
    final ConditionSlot slot = new ConditionSlot(action);
    subscriptions.add(slot);
    return new ActorCondition() {
      @Override
      public void signal() {
        slot.signal();
        wakeSignal();
      }

      @Override
      public void cancel() {
        slot.cancel();
      }
    };
  }

  public <T> void onFutureCompletion(
      final ActorFuture<T> future, final BiConsumer<T, Throwable> consumer) {
    final FutureSlot slot = new FutureSlot(future, (v, e) -> consumer.accept((T) v, e));
    subscriptions.add(slot);
    future.block(this);
  }

  public void onAllFuturesCompletion(
      final List<? extends ActorFuture<?>> futures, final Consumer<Throwable> callback) {
    // 空集合立即回调（旧调度器语义; 否则计数永不到 0, 上游 join 永久挂起）
    if (futures.isEmpty()) {
      callback.accept(null);
      return;
    }
    final AtomicInteger pending = new AtomicInteger(futures.size());
    final List<Throwable> failure = new ArrayList<>(1);
    for (final ActorFuture<?> future : futures) {
      onFutureCompletion(
          future,
          (v, e) -> {
            if (e != null) {
              synchronized (failure) {
                failure.add(e);
              }
            }
            if (pending.decrementAndGet() == 0) {
              callback.accept(failure.isEmpty() ? null : failure.get(0));
            }
          });
    }
  }

  public void onFirstCompletion(
      final List<? extends ActorFuture<?>> futures,
      final BiConsumer<Object, Throwable> callback,
      final Consumer<Object> closer) {
    for (final ActorFuture<?> future : futures) {
      onFutureCompletion(
          future,
          (v, e) -> {
            if (e == null) {
              callback.accept(v, null);
            } else {
              callback.accept(null, e);
            }
          });
    }
  }

  public void addSubscription(final SubscriptionSlot slot) {
    subscriptions.add(slot);
  }

  /** 定时器（owner 线程调用; 路由到当前 runner 的 TimerHub） */
  public ScheduledTimer scheduleTimer(
      final long deadlineMillis,
      final Runnable action,
      final boolean recurring,
      final long periodMillis) {
    final CellRunner runner = currentRunner();
    final TimerSlot slot =
        runner.getTimers().schedule(this, deadlineMillis, action, recurring, periodMillis);
    return runner.getTimers().handle(slot);
  }

  public void runBlocking(final Runnable runnable, final Consumer<Throwable> completion) {
    final CellRunner runner = currentRunner();
    runner
        .getBlocking()
        .submit(
            () -> {
              Throwable error = null;
              try {
                runnable.run();
              } catch (final Throwable t) {
                error = t;
              }
              final Throwable captured = error;
              submitBlockedDone(
                  () -> {
                    if (completion != null) {
                      completion.accept(captured);
                    }
                  });
            });
  }

  // ===== 关闭 / 失败 =====

  public ActorFuture<Void> closeAsync() {
    final CompletableActorFuture<Void> future;
    if (closeFuture == null) {
      closeFuture = new CompletableActorFuture<>();
      final Phases current = phase;
      if (current == Phases.CREATED) {
        // 从未提交调度: 直接终态; 早期入队的 job 无处执行, 逐个 fail 释放 future
        phase = Phases.CLOSED;
        ActorEnvelope queued;
        while ((queued = submitted.poll()) != null) {
          submittedCount.decrementAndGet();
          failEnvelope(queued, new IllegalStateException("Actor is closed"));
        }
        closeFuture.complete(null);
      } else {
        offerInternal(ActorEnvelope.Kind.CLOSE_REQUEST, this::requestClose, null);
        wakeSignal();
      }
    }
    return closeFuture;
  }

  private void requestClose() {
    if (phase == Phases.STARTED) {
      phase = Phases.CLOSE_REQUESTED;
      // fastLane 丢弃并 fail
      ActorEnvelope envelope;
      while ((envelope = fastLane.poll()) != null) {
        failEnvelope(envelope, new IllegalStateException("Actor is closed"));
      }
      actor.internalOnActorCloseRequested();
    } else if (phase == Phases.STARTING) {
      closeRequestedBeforeStarted = true;
    }
  }

  /** 显式失败（control.fail; 任意相位致死） */
  public void failNow(final Throwable error) {
    if (!phase.isTerminal()) {
      transitionFailed(error, null);
    }
  }

  public boolean isClosing() {
    final Phases p = phase;
    return p == Phases.CLOSE_REQUESTED || p == Phases.CLOSING || p.isTerminal();
  }

  public boolean isClosed() {
    return phase.isTerminal();
  }

  // ===== 执行（owner 线程） =====

  /** 排空执行循环：fastLane 优先 → submitted FIFO → 订阅轮询；全空后推进相位并收尾。 */
  public void drain() {
    final int ticketsAtStart = wakeTickets.get();
    while (true) {
      ActorEnvelope envelope = fastLane.poll();
      if (envelope == null) {
        envelope = submitted.poll();
        if (envelope != null) {
          submittedCount.decrementAndGet();
        }
      }
      if (envelope != null) {
        if (!execute(envelope)) {
          return;
        }
        jobExecuted(envelope);
        continue;
      }
      if (pollSubscriptions()) {
        continue;
      }
      break;
    }
    advancePhase();
    finishDrain(ticketsAtStart);
  }

  /**
   * 收尾协议: 先休眠后复查——复查见新工作则自行重新入队（提交者与 owner 只有一个能 CAS 成功, 不会双入队; 提交者在 RUNNING 期间 CAS 失败依赖本复查不丢唤醒）。
   */
  void finishDrain(final int ticketsAtStart) {
    gate.trySleep();
    final boolean newWake = wakeTickets.get() != ticketsAtStart;
    if ((newWake || hasPendingWork()) && gate.tryWakeup()) {
      routeSelf();
    }
  }

  private boolean hasPendingWork() {
    if (!fastLane.isEmpty() || !submitted.isEmpty()) {
      return true;
    }
    for (final SubscriptionSlot slot : subscriptions) {
      if (!slot.isCancelled() && slot.peekDue()) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return false = 致命失败（相位已转 FAILED）
   */
  private boolean execute(final ActorEnvelope envelope) {
    try {
      switch (envelope.getKind()) {
        case RUN, LIFECYCLE -> {
          envelope.getAction().run();
          completeEnvelope(envelope, null);
        }
        case CALL -> {
          final Object result = envelope.getCallable().call();
          completeEnvelope(envelope, result);
        }
        case CLOSE_REQUEST -> envelope.getAction().run();
        case SUBSCRIPTION -> {
          final SubscriptionSlot slot = envelope.getSlot();
          if (slot instanceof FutureSlot futureSlot) {
            futureSlot.runConsumer();
          } else if (slot.getAction() != null) {
            slot.getAction().run();
          }
        }
        case REPEAT -> {
          envelope.getAction().run();
          if (yieldRequested) {
            yieldRequested = false;
            repeatInternal(envelope);
          } else {
            completeEnvelope(envelope, null);
            releaseSelf(envelope);
          }
        }
      }
      return true;
    } catch (final Throwable t) {
      return handleFailure(envelope, t);
    }
  }

  private boolean handleFailure(final ActorEnvelope envelope, final Throwable t) {
    if (phase == Phases.STARTED) {
      // 存活: 记日志 + fail 该 job future
      LOG.error("actor job execute error.", t);
      actor.internalHandleFailure(t);
      failEnvelope(envelope, t);
      return true;
    }
    transitionFailed(t, envelope);
    return false;
  }

  private void transitionFailed(final Throwable error, final ActorEnvelope current) {
    LOG.error("actor transition to FAILED", error);
    phase = Phases.FAILED;
    if (metrics != null) {
      metrics.incActorFailed();
    }
    ActorEnvelope envelope;
    while ((envelope = fastLane.poll()) != null) {
      failEnvelope(envelope, error);
    }
    while ((envelope = submitted.poll()) != null) {
      submittedCount.decrementAndGet();
      failEnvelope(envelope, error);
    }
    if (startingFuture != null && !startingFuture.isDone()) {
      startingFuture.completeExceptionally(error);
    }
    if (closeFuture != null && !closeFuture.isDone()) {
      closeFuture.completeExceptionally(error);
    }
    try {
      actor.internalOnActorFailed();
    } catch (final Throwable t) {
      LOG.error("onActorFailed hook failed", t);
    }
  }

  @SuppressWarnings("unchecked")
  private void completeEnvelope(final ActorEnvelope envelope, final Object result) {
    final ActorFuture<Object> future = envelope.getFuture();
    if (future != null) {
      future.complete(result);
    }
  }

  @SuppressWarnings("unchecked")
  private void failEnvelope(final ActorEnvelope envelope, final Throwable error) {
    final ActorFuture<Object> future = envelope.getFuture();
    if (future != null) {
      future.completeExceptionally(error);
    }
  }

  private void advancePhase() {
    if (phase == Phases.STARTING) {
      phase = Phases.STARTED;
      if (metrics != null) {
        metrics.incActorStarted();
      }
      if (startingFuture != null && !startingFuture.isDone()) {
        startingFuture.complete(null);
      }
      offerInternal(ActorEnvelope.Kind.LIFECYCLE, actor::internalOnActorStarted, null);
      if (closeRequestedBeforeStarted) {
        closeRequestedBeforeStarted = false;
        requestClose();
      }
    } else if (phase == Phases.CLOSE_REQUESTED) {
      phase = Phases.CLOSING;
      offerInternal(ActorEnvelope.Kind.LIFECYCLE, actor::internalOnActorClosing, null);
    } else if (phase == Phases.CLOSING) {
      phase = Phases.CLOSED;
      if (metrics != null) {
        metrics.incActorClosed();
      }
      offerInternal(ActorEnvelope.Kind.LIFECYCLE, this::runOnActorClosedAndCleanup, null);
    }
  }

  private void runOnActorClosedAndCleanup() {
    try {
      actor.internalOnActorClosed();
    } finally {
      for (final SubscriptionSlot slot : subscriptions) {
        // 未消费的 future 续接不能被静默丢弃: 订阅列表即将清空, 轮询永不再发生,
        // 不异常完成消费方会导致上游 future 完成丢失（链永挂、无日志）。
        // 必须先于 cancel() 执行（failConsumer 对已取消槽位是 no-op）
        if (slot instanceof FutureSlot futureSlot) {
          try {
            futureSlot.failConsumer(
                new IllegalStateException("Actor '" + getName() + "' is closed"));
          } catch (final RuntimeException e) {
            LOG.error("Failing pending future subscription on close failed", e);
          }
        }
        slot.cancel();
      }
      subscriptions.clear();
      ActorEnvelope envelope;
      while ((envelope = submitted.poll()) != null) {
        submittedCount.decrementAndGet();
        failEnvelope(envelope, new IllegalStateException("Actor is closed"));
      }
      if (closeFuture != null && !closeFuture.isDone()) {
        closeFuture.complete(null);
      }
    }
  }

  /**
   * @return 任一订阅到期已消费则 true
   *     <p>索引循环而非迭代器：订阅动作内可能再注册新订阅（如条件回调里 runOnCompletion）， 迭代器会抛
   *     CME；索引循环容忍遍历中增删（新增的追加在尾部，本轮或下轮消费）。
   */
  private boolean pollSubscriptions() {
    boolean anyDue = false;
    for (int i = 0; i < subscriptions.size(); i++) {
      final SubscriptionSlot slot = subscriptions.get(i);
      if (slot.isCancelled()) {
        subscriptions.remove(i);
        i--;
        continue;
      }
      if (slot.pollDue()) {
        anyDue = true;
        final ActorEnvelope envelope = takeSelf();
        envelope.wrapSubscription(slot);
        if (!execute(envelope)) {
          return anyDue;
        }
        jobExecuted(envelope);
        releaseIfDisposable(envelope);
        if (slot instanceof FutureSlot) {
          subscriptions.remove(i);
          i--;
        }
      }
    }
    return anyDue;
  }

  // ===== 指标 =====

  private void jobSubmitted() {
    if (metrics != null && pool != null) {
      metrics.incJobSubmitted(pool.poolName());
    }
  }

  /** 生命周期/关闭信封不计入 job 执行指标 */
  private void jobExecuted(final ActorEnvelope envelope) {
    if (metrics != null
        && pool != null
        && envelope.getKind() != ActorEnvelope.Kind.LIFECYCLE
        && envelope.getKind() != ActorEnvelope.Kind.CLOSE_REQUEST) {
      metrics.incJobExecuted(pool.poolName());
    }
  }

  private void rejectJob(final String reason) {
    if (metrics != null && pool != null) {
      metrics.incJobRejected(pool.poolName(), reason);
    }
  }

  // ===== owner 私有 =====

  private ActorEnvelope takeSelf() {
    final ActorEnvelope envelope = selfPool.poll();
    return envelope != null ? envelope : new ActorEnvelope();
  }

  /** RUN/LIFECYCLE/SUBSCRIPTION 一次性信封执行后回 owner 池 */
  private void releaseIfDisposable(final ActorEnvelope envelope) {
    if (envelope.getKind() != ActorEnvelope.Kind.REPEAT
        && envelope.getKind() != ActorEnvelope.Kind.CLOSE_REQUEST) {
      releaseSelf(envelope);
    }
  }

  private void releaseSelf(final ActorEnvelope envelope) {
    envelope.reset();
    if (selfPool.size() < SELF_POOL_CAP) {
      selfPool.push(envelope);
    }
  }

  private void offerInternal(
      final ActorEnvelope.Kind kind, final Runnable action, final ActorFuture<?> future) {
    final ActorEnvelope envelope = takeSelf();
    envelope.wrap(kind, action, future);
    fastLane.addLast(envelope);
  }

  private CellRunner currentRunner() {
    final CellRunner runner = homeRunner;
    if (runner != null && runner.isOnOwnerThread()) {
      return runner;
    }
    throw new IllegalStateException(
        "actor operation must be called from within actor " + getName());
  }

  /** runUntilDone 的让步标记 */
  public void yieldThread() {
    yieldRequested = true;
  }
}
