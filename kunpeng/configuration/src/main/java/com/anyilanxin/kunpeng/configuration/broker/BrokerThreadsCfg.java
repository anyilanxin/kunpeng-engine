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
package com.anyilanxin.kunpeng.configuration.broker;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Broker 线程配置，定义 CPU 线程数与 IO 线程数。
 *
 * <p>CPU 线程默认与调度器构建器一致（{@code max(1, 核数 - 2)}）：一个节点上所有分区的处理 actor 与定时车道都跑在这组线程上，
 * 写死小值会让处理能力与核数无关。每条线程空闲时约占 2% 个核（每毫秒一次的空闲轮询，定时精度所依赖）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public final class BrokerThreadsCfg implements ConfigurationEntry {
  private int cpuThreadCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
  private int ioThreadCount = 2;
}
