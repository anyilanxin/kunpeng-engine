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
package com.anyilanxin.kunpeng.engine.bpmn;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import org.agrona.collections.IntHashSet;

/**
 * 支持向其他分区发送任意命令。命令的目标分区确认写入其日志之前一直被跟踪：目标分区明确没收下或命令没发到它的 leader 时自动重发；等不到确认的命令可能已写入，不重发。
 *
 * <p>因此发送仍不是端到端可靠的：调用方需自行检测没有达成预期结果的命令并重试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface InterPartitionCommandSender {
  void sendCommand(
      final int receiverResourceId,
      final ValueLifeCycle lifeCycle,
      final UnifiedRecordValue command);

  /**
   * 写入命令时使用指定的 record key，其余行为与 {@link InterPartitionCommandSender#sendCommand} 一致。适用于 key
   * 已暴露给用户、实体必须以该 key 出现在接收分区的场景；同时也能告知接收分区该命令来自其他分区（key 中编码了分区 id，可据此判断来源分区）。
   *
   * @param recordKey 写入命令使用的 record key，为 null 时忽略
   */
  void sendCommand(
      final int receiverResourceId,
      final ValueLifeCycle lifeCycle,
      final Long recordKey,
      final UnifiedRecordValue command);

  /**
   * 写入命令时使用指定的 record key，其余行为与 {@link InterPartitionCommandSender#sendCommand} 一致。适用于 key
   * 已暴露给用户、实体必须以该 key 出现在接收分区的场景；同时也能告知接收分区该命令来自其他分区（key 中编码了分区 id，可据此判断来源分区）。
   *
   * @param recordKey 写入命令使用的 record key，为 null 时忽略
   */
  void sendCommand(
      final int receiverResourceId,
      final ValueLifeCycle lifeCycle,
      final Long recordKey,
      final Long operationReference,
      final UnifiedRecordValue command);

  IntHashSet getActivitySourceIds();

  IntHashSet getActivityPartitionIds();

  int getPartitionSource(final int partitionId);
}
