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
package com.anyilanxin.kunpeng.broker.client.core;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;

/**
 * 在途请求因目标分区的 leader 变更而被网关终止：原 leader 已不可能应答（进程崩溃、被隔离后退位）。
 *
 * <p>命令可能已进入日志并被新 leader 执行，结果未知。与 broker 应答码 {@link BrokerResponseCodes#LEADER_STEPPED_DOWN}
 * 同义，不应盲目重试非幂等命令。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class LeaderChangedException extends Exception {
  public LeaderChangedException(final PartitionId partitionId) {
    super(
        "Leader of partition "
            + partitionId
            + " changed before the command was answered; the command may have been applied");
  }
}
