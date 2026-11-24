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
 * along with this program.  If not, see <http://www.gnu.org/licenses>.
 */
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.eventlog.serialize.BatchFrame;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedType;
import java.util.Arrays;
import java.util.List;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 批内命令快照以入批时的编码字节原样写出：得到的批帧必须与“把入批时刻的源记录直接交给批帧编码”逐字节一致，
 * 源记录之后被改写也不影响批内内容。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("调度命令批内快照透传")
class CommandRecordSnapshotTest {

  @Test
  @DisplayName("经批内快照写出的批帧与直接写源记录逐字节一致")
  void batchFrameIsByteIdenticalToWritingTheSourceRecordsDirectly() {
    final var batch = new HeapCommandBatch((count, bytes) -> true);
    final var first = delayed(51, 900);
    final var second = delayed(52, 1_200);
    assertTrue(batch.append(51, -1, commandMetadata(DelayedLifeCycle.TRIGGER), first).isRight());
    assertTrue(batch.append(52, -1, commandMetadata(DelayedLifeCycle.TRIGGER), second).isRight());
    // 入批后改写源记录：批内快照不受影响
    first.setDelayedId(999).setDueDate(1_900);

    final var direct =
        List.of(
            AppendEntry.of(51, commandMetadata(DelayedLifeCycle.TRIGGER), delayed(51, 900)),
            AppendEntry.of(52, commandMetadata(DelayedLifeCycle.TRIGGER), delayed(52, 1_200)));

    assertArrayEquals(frame(direct), frame(batch.entries()));
  }

  @Test
  @DisplayName("探针拒绝的记录不入批，也不计入字节数")
  void rejectedRecordIsNotAppended() {
    final var batch = new HeapCommandBatch((count, bytes) -> count == 1);
    assertTrue(batch.append(51, -1, commandMetadata(DelayedLifeCycle.TRIGGER), delayed(51, 900)).isRight());
    final int firstLength = batch.byteSize();

    final var rejected =
        batch.append(52, -1, commandMetadata(DelayedLifeCycle.TRIGGER), delayed(52, 1_200));

    assertTrue(rejected.isLeft());
    assertEquals(1, batch.entries().size());
    assertEquals(firstLength, batch.byteSize());
  }

  private static byte[] frame(final List<AppendEntry> entries) {
    var payload = 0;
    for (final AppendEntry entry : entries) {
      payload += entry.getLength();
    }
    final var buffer =
        new UnsafeBuffer(new byte[BatchFrame.maximumSerializedLength(entries.size(), payload)]);
    final int end =
        BatchFrame.serialize(buffer, 0, 17, 5, 1_760_000_000_000L, List.copyOf(entries));
    return Arrays.copyOf(buffer.byteArray(), end);
  }

  private static AdminRecordMetadata commandMetadata(final AdminValueLifeCycle lifeCycle) {
    final var metadata = new AdminRecordMetadata();
    metadata
        .valueLifeCycle(lifeCycle)
        .recordType(RecordType.COMMAND)
        .valueType(lifeCycle.getValueType())
        .recordVersion(AdminRecordMetadata.DEFAULT_RECORD_VERSION);
    return metadata;
  }

  private static DelayedRecord delayed(final long delayedId, final long dueDate) {
    // structpack 序列化要求枚举属性必须显式设置
    return new DelayedRecord()
        .setDelayedId(delayedId)
        .setDueDate(dueDate)
        .setPartitionType(PartitionType.BUSINESS)
        .setDispatchPlanId(delayedId * 10)
        .setDelayedType(DelayedType.BUSINESS_EXECUTION);
  }
}
