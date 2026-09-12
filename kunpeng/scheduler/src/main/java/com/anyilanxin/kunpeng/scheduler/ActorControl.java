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

import com.anyilanxin.kunpeng.scheduler.clock.ActorClock;
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

  /**
   * 提交动作到本 actor 执行。
   *
   * <p>提交必达：队列无界，不因积压被拒；仅 actor 关闭/失败时 future 异常完成。调用方应关注 返回的 future 以感知失败，而非默认提交一定成功。
   *
   * @return 动作完成 future
   */
  public ActorFuture<Void> run(final Runnable action) {
    if (isCalledFromWithinActor()) {
      return cell.submitInternal(ActorEnvelope.Kind.RUN, action, null).thenApply(ignored -> null);
    }
    return cell.submitExternal(ActorEnvelope.Kind.RUN, action, null).thenApply(ignored -> null);
  }

  public ActorFuture<Void> submit(final Runnable action) {
    return run(action);
  }

  public <T> ActorFuture<T> call(final Callable<T> callable) {
    return cell.submitExternal(ActorEnvelope.Kind.CALL, null, callable);
  }

  public ActorFuture<Void> call(final Runnable action) {
    return run(action);
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
    // 必须用载体时钟: 定时轮按 clock.getTimeMillis() 轮询, 受控时钟（如测试 pin 时间）下
    // 混用系统时钟会导致 timer 永不触发或立即触发
    final long deadline = ActorClock.currentTimeMillis() + delay.toMillis();
    return cell.scheduleTimer(deadline, runnable, false, 0);
  }

  public ScheduledTimer runAt(final long timestamp, final Runnable runnable) {
    ensureWithinActor();
    return cell.scheduleTimer(timestamp, runnable, false, 0);
  }

  public ScheduledTimer runAtFixedRate(final Duration period, final Runnable runnable) {
    ensureWithinActor();
    final long deadline = ActorClock.currentTimeMillis() + period.toMillis();
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
      throw new IllegalStateException("操作必须在 actor " + actor.getName() + " 内调用");
    }
  }
}
