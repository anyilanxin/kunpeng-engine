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
package com.anyilanxin.kunpeng.engine.bpmn.scheduling;

import com.anyilanxin.kunpeng.protocol.business.FollowUpCommandMetadata;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.List;

/**
 * 将命令缓冲进 {@link HeapCommandBatch}、并针对 {@link PendingCommandRegistry.Staging} 会话去重的 {@link
 * CommandCollector}。
 *
 * <p>去重是短路语义：同一 {@link ValueLifeCycle lifeCycle} 与 key 的命令若已在途，则静默接受、
 * 不触碰批次，调用方可将其视为空操作追加。通过去重的命令被复制进批次并登记到暂存会话； 暂存会话仅在其属主调用 {@link
 * PendingCommandRegistry.Staging#commit()} 时才合并进主 注册表。
 *
 * <p>大小估算委托给批次完成：先以通用元数据形状推导每条目的额外开销，再征询容量探针。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BufferedCommandCollector implements CommandCollector {

  private static final RecordValueMapper VALUE_MAPPER = DefaultRecordValueMapper.getInstance();

  private final HeapCommandBatch batch;
  private final PendingCommandRegistry.Staging staged;

  /**
   * 创建向新批次缓冲、并经 {@code staged} 去重的收集器。
   *
   * @param capacityProbe 每次追加与大小估算都会征询的容量探针
   * @param staged 记录已接受命令 key 的暂存会话
   */
  public BufferedCommandCollector(
      final CommandBatch.CapacityProbe capacityProbe, final PendingCommandRegistry.Staging staged) {
    batch = new HeapCommandBatch(capacityProbe);
    this.staged = staged;
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public boolean appendCommand(
      final long key,
      final ValueLifeCycle lifeCycle,
      final UnifiedRecordValue value,
      final FollowUpCommandMetadata metadata) {
    if (staged.contains(lifeCycle, key)) {
      return true;
    }

    final RecordMetadata recordMetadata = new RecordMetadata();
    // recordType 缺省为 NULL_VAL，处理端只分发 COMMAND/COMMAND_API，缺失会被静默跳过
    recordMetadata
        .valueType(lifeCycle.getValueType())
        .recordType(RecordType.COMMAND)
        .valueLifeCycle(lifeCycle)
        .rejectionType("")
        .rejectionReason("")
        .recordVersion(RecordMetadata.DEFAULT_RECORD_VERSION)
        .brokerVersion(RecordMetadata.CURRENT_BROKER_VERSION)
        .operationReference(metadata.operationReference());

    final Either<RuntimeException, Void> appended =
        batch.append(key, -1, recordMetadata, VALUE_MAPPER.copyValue(lifeCycle, value));
    appended.ifRight(ignored -> staged.add(lifeCycle, key));
    return appended.isRight();
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public boolean canAppend(
      final List<? extends UnifiedRecordValue> values, final FollowUpCommandMetadata metadata) {
    if (values.isEmpty()) {
      return true;
    }

    final RecordMetadata genericMetadata = new RecordMetadata();
    genericMetadata
        .valueType(ValueType.UNKNOW)
        .valueLifeCycle(ValueLifeCycle.UnknownState.UNKNOWN)
        .rejectionType("")
        .rejectionReason("")
        .operationReference(metadata.operationReference());

    final UnifiedRecordValue firstValue = values.get(0);
    final com.anyilanxin.kunpeng.engine.bpmn.scheduling.CommandRecord firstRecord =
        new CommandRecord(NO_KEY, -1, genericMetadata, firstValue);
    final int overhead = firstRecord.getLength() - firstValue.getLength();

    int totalLength = firstRecord.getLength();
    for (int i = 1; i < values.size(); i++) {
      totalLength += values.get(i).getLength() + overhead;
    }

    return batch.admits(totalLength);
  }

  @Override
  public CommandBatch build() {
    return batch;
  }
}
