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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * 三态调度门（防丢唤醒核心）：
 *
 * <pre>
 * WAITING --(提交者 CAS 成功, 负责把 cell 入 runner 队列)--> WAKING_UP --(runner 取走)--> RUNNING
 * RUNNING --(owner 排空后写回)--> WAITING；owner 写回后复查队列, 非空则自行重新 WAKING_UP
 * </pre>
 *
 * <p>不变量：每个 WAITING→WAKING_UP 片段恰有一个入队者（CAS 唯一胜者），因此队列中同一 cell 至多一个实例——物理移除式偷取下天然单线程执行, 无需旧版
 * stateCount claim 竞争。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SchedulingGate {

  private static final VarHandle STATE;

  static {
    try {
      STATE = MethodHandles.lookup().findVarHandle(SchedulingGate.class, "state", int.class);
    } catch (final ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static final int WAITING = 0;
  public static final int WAKING_UP = 1;
  public static final int RUNNING = 2;

  @SuppressWarnings("unused")
  private volatile int state = WAITING;

  /**
   * @return true 表示调用者是本片段的唤醒责任人（负责入队）
   */
  public boolean tryWakeup() {
    return STATE.compareAndSet(this, WAITING, WAKING_UP);
  }

  /**
   * runner 从队列取走 cell 时立即调用（WAKING_UP→RUNNING 原子接管）。
   *
   * <p>必须是 CAS：取走（poll）与接管之间 submitter 可能恰好 CAS 成功再次入队， 产生同一 cell 的第二份实例； CAS 失败即说明发生并发接管。
   *
   * @return false 表示门不在 WAKING_UP（平台池下不可能发生：每个 WAITING→WAKING_UP 片段恰一个入队者，队列中同一 cell 至多一份实例）
   */
  public boolean markRunning() {
    return STATE.compareAndSet(this, WAKING_UP, RUNNING);
  }

  /**
   * owner 排空后尝试休眠。
   *
   * <p>必须是 volatile 写，不能是 release 写：调用方写入 WAITING 之后要复查票差与队列（{@code ActorCell.finishDrain}），而
   * release 写只约束它之前的写不后移，不约束它之后的读不前移。缺 StoreLoad 屏障时，WAITING 尚未全局可见复查就已读到旧值，与此同时并发提交者的 CAS 仍见到旧的
   * RUNNING 而失败——两边都不入队，cell 永久停在 WAITING 而队列里有任务（多 runner 下实测每次跨线程投递约 4e-6~4e-5 命中）。
   *
   * <p>volatile 写同时把 drain 期间的普通写（相位、fastLane、订阅槽等）发布给随后经 CAS 接管的其他 runner。
   *
   * @return true 表示回到 WAITING（调用方须复查队列, 非空则 tryWakeup 重入队）
   */
  public boolean trySleep() {
    STATE.setVolatile(this, WAITING);
    return true;
  }

  public int state() {
    return (int) STATE.getAcquire(this);
  }
}
