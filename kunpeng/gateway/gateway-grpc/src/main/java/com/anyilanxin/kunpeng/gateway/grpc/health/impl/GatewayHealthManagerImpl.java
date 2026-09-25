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
package com.anyilanxin.kunpeng.gateway.grpc.health.impl;

import com.anyilanxin.kunpeng.gateway.grpc.health.GatewayHealthManager;
import com.anyilanxin.kunpeng.gateway.grpc.health.Status;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import io.grpc.BindableService;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.jcip.annotations.ThreadSafe;

/**
 * 聚合各 gRPC 服务的生命周期状态，并映射为 gRPC 标准健康检查服务。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@ThreadSafe
public final class GatewayHealthManagerImpl implements GatewayHealthManager {

  private final HealthStatusManager grpcHealth;
  private final Set<String> watchedServices = new HashSet<>();
  private final AtomicReference<Status> lifecycle = new AtomicReference<>(Status.INITIAL);

  public GatewayHealthManagerImpl(final List<GrpcService> services) {
    grpcHealth = new HealthStatusManager();
    for (final GrpcService service : services) {
      watchedServices.add(service.getServiceName());
    }
    watchedServices.add(HealthStatusManager.SERVICE_NAME_ALL_SERVICES);
    // HealthStatusManager 出厂即回报 SERVING，这里先统一压成 NOT_SERVING，等启动流程推进后再放开。
    publish(ServingStatus.NOT_SERVING);
  }

  @Override
  public Status getStatus() {
    return lifecycle.get();
  }

  @Override
  public void setStatus(final Status next) {
    final Status previous = lifecycle.getAndAccumulate(next, this::advance);
    // SHUTDOWN 为终态且不再广播；状态未变化时也无需重复发布。
    if (previous == Status.SHUTDOWN || previous == next) {
      return;
    }
    broadcast(next);
  }

  @Override
  public BindableService getHealthService() {
    return grpcHealth.getHealthService();
  }

  private Status advance(final Status current, final Status next) {
    return current == Status.SHUTDOWN ? Status.SHUTDOWN : next;
  }

  private void broadcast(final Status status) {
    if (status == Status.RUNNING) {
      publish(ServingStatus.SERVING);
    } else if (status == Status.SHUTDOWN) {
      grpcHealth.enterTerminalState();
    } else {
      publish(ServingStatus.NOT_SERVING);
    }
  }

  private void publish(final ServingStatus servingStatus) {
    for (final String service : watchedServices) {
      grpcHealth.setStatus(service, servingStatus);
    }
  }
}
