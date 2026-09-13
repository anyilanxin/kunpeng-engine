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
package com.anyilanxin.kunpeng.gateway;

import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.configuration.gateway.GatewayCfg;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import com.anyilanxin.kunpeng.utils.CloseableSilently;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * gateway
 *
 * @author zxuanhong
 * @since
 */
public class Gateway implements CloseableSilently {
  private final ActorSchedulingService schedulingService;
  private final GatewayCfg gatewayCfg;
  private final AtomixCluster atomixCluster;
  private final MeterRegistry meterRegistry;

  public Gateway(
      final ActorSchedulingService schedulingService,
      final GatewayCfg gatewayCfg,
      final AtomixCluster atomixCluster,
      final MeterRegistry meterRegistry) {
    this.schedulingService = schedulingService;
    this.gatewayCfg = gatewayCfg;
    this.atomixCluster = atomixCluster;
    this.meterRegistry = meterRegistry;
  }

  public ActorFuture<Gateway> start() {
    return CompletableActorFuture.completed(this);
  }

  @Override
  public void close() {}
}
