/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

import com.anyilanxin.kunpeng.scheduler.Actor;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 为一条执行车道托管单个 {@link OrderedTimerScheduler} 的 actor。
 *
 * <p>调度器随 actor 启动而绑定、随 actor 关闭而解绑；绑定与解绑均为同步操作，因此使用 actor 基类的默认生命周期即可。
 */
final class LaneActor extends Actor {

  private final ExecutionLane lane;
  private final int partitionId;
  private final OrderedTimerScheduler scheduleService;
  private final Consumer<LaneActor> onFailure;

  LaneActor(
      final ExecutionLane lane,
      final TimerSchedulerFactory factory,
      final int partitionId,
      final Consumer<LaneActor> onFailure) {
    this.lane = lane;
    this.partitionId = partitionId;
    scheduleService = factory.create();
    this.onFailure = onFailure;
  }

  /** 返回本车道 actor 的描述性名称。 */
  public String name() {
    return Actor.buildActorName(lane.label(), partitionId);
  }

  /** 返回本车道所属的执行车道。 */
  public ExecutionLane lane() {
    return lane;
  }

  /** 返回本车道 actor 托管的定时调度器。 */
  public TimerScheduler scheduler() {
    return scheduleService;
  }

  @Override
  public String getName() {
    return name();
  }

  @Override
  protected Map<String, String> createContext() {
    final Map<String, String> context = super.createContext();
    context.put("partitionId", String.valueOf(partitionId));
    return context;
  }

  @Override
  protected void onActorStarting() {
    scheduleService.attach(actor);
  }

  @Override
  protected void onActorClosing() {
    scheduleService.detach();
  }

  @Override
  protected void onActorFailed() {
    // 在已失败 actor 的载体线程上回调; 实现必须非阻塞
    onFailure.accept(this);
  }
}
