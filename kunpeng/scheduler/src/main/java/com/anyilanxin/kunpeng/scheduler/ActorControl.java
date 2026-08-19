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
package com.anyilanxin.kunpeng.scheduler;

import com.anyilanxin.kunpeng.scheduler.core.ActorCell;
import com.anyilanxin.kunpeng.scheduler.core.ActorEnvelope;
import com.anyilanxin.kunpeng.scheduler.core.CarrierContext;
import com.anyilanxin.kunpeng.scheduler.core.Phases;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** actor 控制面：actor 内部线程调用（条件/定时器/续接/关闭） */
public final class ActorControl implements ConcurrencyControl {

  private final Actor actor;
  private final ActorCell cell;

  public ActorControl(final Actor actor) {
    this.actor = actor;
    // name/context 延迟到消费期解析（ActorControl 字段初始化先于子类构造器）
    this.cell = new ActorCell(actor);
    this.cell.setControl(this);
  }

  public static ActorControl current() {
    final CarrierContext context = CarrierContext.current();
    return context != null ? context.currentControl() : null;
  }

  public ActorCell getCell() {
    return cell;
  }

  public boolean isCalledFromWithinActor() {
    final ActorControl current = current();
    return current == this;
  }

  // ===== 提交 =====

  public void run(final Runnable action) {
    if (isCalledFromWithinActor()) {
      cell.submitInternal(ActorEnvelope.Kind.RUN, action, null);
    } else {
      cell.submitExternal(ActorEnvelope.Kind.RUN, action, null);
    }
  }

  public void submit(final Runnable action) {
    run(action);
  }

  public <T> ActorFuture<T> call(final Callable<T> callable) {
    return cell.submitExternal(ActorEnvelope.Kind.CALL, null, callable);
  }

  public ActorFuture<Void> call(final Runnable action) {
    return cell.submitExternal(ActorEnvelope.Kind.RUN, action, null).thenApply(ignored -> null);
  }

  /** runUntilDone: runnable 内调用 yieldThread() 请求重跑, 否则结束 */
  public void runUntilDone(final Runnable runnable) {
    cell.submitInternal(ActorEnvelope.Kind.REPEAT, runnable, null);
  }

  public void yieldThread() {
    cell.yieldThread();
  }

  /** 阻塞动作外包（完成回投 actor） */
  public void runBlocking(final Runnable runnable) {
    runBlocking(runnable, null);
  }

  public void runBlocking(final Runnable runnable, final Consumer<Throwable> completionConsumer) {
    cell.runBlocking(runnable, completionConsumer);
  }

  // ===== 订阅（须在 actor 线程） =====

  public ActorCondition onCondition(final String conditionName, final Runnable conditionAction) {
    ensureWithinActor();
    return cell.onCondition(conditionName, conditionAction);
  }

  public <T> void runOnCompletion(
      final ActorFuture<T> future, final BiConsumer<T, Throwable> callback) {
    ensureWithinActor();
    cell.onFutureCompletion(future, callback);
  }

  public <T> void runOnCompletion(
      final Collection<ActorFuture<T>> futures, final Consumer<Throwable> callback) {
    ensureWithinActor();
    cell.onAllFuturesCompletion(List.copyOf(futures), callback);
  }

  /** 与 runOnCompletion 一致（旧 API 的相位变体） */
  public <T> void runOnCompletionBlockingCurrentPhase(
      final ActorFuture<T> future, final BiConsumer<T, Throwable> callback) {
    runOnCompletion(future, callback);
  }

  public <T> void runOnFirstCompletion(
      final Collection<ActorFuture<T>> futures,
      final BiConsumer<T, Throwable> callback,
      final Consumer<T> closer) {
    ensureWithinActor();
    cell.onFirstCompletion(
        List.copyOf(futures), (v, e) -> callback.accept((T) v, e), v -> closer.accept((T) v));
  }

  public <T> void runOnFirstCompletion(
      final Collection<ActorFuture<T>> futures, final Consumer<T> callback) {
    runOnFirstCompletion(futures, (v, e) -> callback.accept(v), v -> {});
  }

  // ===== 定时器（须在 actor 线程） =====

  public ScheduledTimer schedule(final Duration delay, final Runnable runnable) {
    ensureWithinActor();
    final long deadline = System.currentTimeMillis() + delay.toMillis();
    return cell.scheduleTimer(deadline, runnable, false, 0);
  }

  public ScheduledTimer runAt(final long timestamp, final Runnable runnable) {
    ensureWithinActor();
    return cell.scheduleTimer(timestamp, runnable, false, 0);
  }

  public ScheduledTimer runAtFixedRate(final Duration period, final Runnable runnable) {
    ensureWithinActor();
    final long deadline = System.currentTimeMillis() + period.toMillis();
    return cell.scheduleTimer(deadline, runnable, true, period.toMillis());
  }

  // ===== 生命周期 =====

  public ActorFuture<Void> close() {
    return cell.closeAsync();
  }

  public boolean isClosing() {
    return cell.isClosing();
  }

  public boolean isClosed() {
    return cell.isClosed();
  }

  public Phases getLifecyclePhase() {
    return cell.getPhase();
  }

  public void fail(final Throwable error) {
    cell.failNow(error);
  }

  private void ensureWithinActor() {
    if (!isCalledFromWithinActor()) {
      throw new IllegalStateException(
          "Operation must be called from within actor " + actor.getName());
    }
  }
}
