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
package com.anyilanxin.kunpeng.cluster.dispatch;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import java.util.Set;

/** 支持向其他 partition 发送任意 command。发送可能不可靠且会静默失败，需要由调用方自行检测并重试。 */
public interface InterPartitionCommandSender {
  void sendCommand(
      final int receiverResourceId,
      final AdminValueLifeCycle lifeCycle,
      final UnifiedRecordValue command);

  /**
   * 写入 command 时使用给定的 record key。其余行为与 {@link InterPartitionCommandSender#sendCommand} 相同。适用于 key
   * 由上层使用者提供的场景，此时实体必须以该特定 key 存在于接收端 partition 上。这也能让接收端 partition 得知该 command 来自另一个 partition（key
   * 中编码了 partition id，因而可知具体来源）。
   *
   * @param recordKey 写入 command 时使用的 record key。为 null 时忽略。
   */
  void sendCommand(
      final int receiverResourceId,
      final AdminValueLifeCycle lifeCycle,
      final Long recordKey,
      final UnifiedRecordValue command);

  /**
   * 写入 command 时使用给定的 record key。其余行为与 {@link InterPartitionCommandSender#sendCommand} 相同。适用于 key
   * 由上层使用者提供的场景，此时实体必须以该特定 key 存在于接收端 partition 上。这也能让接收端 partition 得知该 command 来自另一个 partition（key
   * 中编码了 partition id，因而可知具体来源）。
   *
   * @param recordKey 写入 command 时使用的 record key。为 null 时忽略。
   */
  void sendCommand(
      final int receiverResourceId,
      final AdminValueLifeCycle lifeCycle,
      final Long recordKey,
      final Long operationReference,
      final UnifiedRecordValue command);

  Set<Integer> getActivitySourceIds();
}
