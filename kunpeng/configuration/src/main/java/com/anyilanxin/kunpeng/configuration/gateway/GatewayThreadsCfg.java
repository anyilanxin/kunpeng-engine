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
package com.anyilanxin.kunpeng.configuration.gateway;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** gateway 线程配置，定义管理线程数与 gRPC 线程池大小。 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public final class GatewayThreadsCfg {

  private final int managementThreads = ConfigurationDefaults.DEFAULT_MANAGEMENT_THREADS;
  private final int grpcMinThreads = Runtime.getRuntime().availableProcessors();
  private final int grpcMaxThreads = 2 * Runtime.getRuntime().availableProcessors();
}
