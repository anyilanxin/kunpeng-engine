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
package com.anyilanxin.kunpeng.gateway.grpc.health;

import io.grpc.BindableService;

/**
 * 网关健康状态聚合：对外提供线程安全的状态读写，并挂载 gRPC 标准健康检查服务。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface GatewayHealthManager {

  /**
   * 线程安全地读取网关当前健康状态。
   *
   * @return 当前健康状态
   */
  Status getStatus();

  /**
   * 以线程安全方式更新网关健康状态。
   *
   * @param status 网关的最新健康状态
   */
  void setStatus(final Status status);

  /**
   * 返回可注册到 {@link io.grpc.ServerBuilder} 的健康检查服务。
   *
   * @return 可绑定的 gRPC 服务
   */
  BindableService getHealthService();
}
