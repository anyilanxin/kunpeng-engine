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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.fromBytes;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.toBytes;

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import java.util.function.Supplier;

/**
 * @author zxuanhong
 * @since
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ExecutionRecordSerialize {
  private static final Supplier<UnifiedRecordValue>[] recordValues;

  static {
    recordValues = new Supplier[PartitionExecutionType.getMaxValue() + 1];
    recordValues[PartitionExecutionType.BOOTSTRAP.getValue()] = PartitionBootstrapRecord::new;
    recordValues[PartitionExecutionType.JOIN.getValue()] = PartitionJoinRecord::new;
    recordValues[PartitionExecutionType.LEAVE.getValue()] = PartitionLeaveRecord::new;
    recordValues[PartitionExecutionType.STOP.getValue()] = PartitionStopRecord::new;
    recordValues[PartitionExecutionType.CONFIG_CHANGE.getValue()] =
        PartitionConfigChangeRecord::new;
    recordValues[PartitionExecutionType.LEAVE_SOURCE_DATA_TRANSFER.getValue()] =
        PartitionLeaveSourceDataTransferRecord::new;
    recordValues[PartitionExecutionType.LEAVE_SOURCE_TRANSFER.getValue()] =
        PartitionLeaveSourceTransferRecord::new;
    recordValues[PartitionExecutionType.BOOTSTRAP_SOURCE_DATA_TRANSFER.getValue()] =
        PartitionBootstrapSourceDataTransferRecord::new;
    recordValues[PartitionExecutionType.BOOTSTRAP_SOURCE_TRANSFER.getValue()] =
        PartitionBootstrapSourceTransferRecord::new;
    recordValues[PartitionExecutionType.SOURCE_DATA_TRANSFER.getValue()] =
        PartitionSourceDataTransferRecord::new;
    recordValues[PartitionExecutionType.SOURCE_TRANSFER.getValue()] =
        PartitionSourceTransferRecord::new;
  }

  /** 解码 */
  public static <T extends UnifiedRecordValue> T decode(
      final byte[] bytes, final PartitionExecutionType executionType) {
    final UnifiedRecordValue unifiedRecordValue = recordValues[executionType.getValue()].get();
    return (T) fromBytes(bytes, unifiedRecordValue);
  }

  /** 解码 */
  public static <T extends UnifiedRecordValue> T decode(final byte[] bytes, final T recordValue) {
    return fromBytes(bytes, recordValue);
  }

  /** 编码 */
  public static <T extends UnifiedRecordValue> byte[] encode(final T recordValue) {
    return toBytes(recordValue);
  }

  public static PartitionExecutionAckRecord decodePartitionExecutionAck(final byte[] bytes) {
    final PartitionExecutionAckRecord unifiedRecordValue = new PartitionExecutionAckRecord();
    return fromBytes(bytes, unifiedRecordValue);
  }

  public static NodeSourceApplyRecord decodeNodeSourceApply(final byte[] bytes) {
    final NodeSourceApplyRecord unifiedRecordValue = new NodeSourceApplyRecord();
    return fromBytes(bytes, unifiedRecordValue);
  }
}
