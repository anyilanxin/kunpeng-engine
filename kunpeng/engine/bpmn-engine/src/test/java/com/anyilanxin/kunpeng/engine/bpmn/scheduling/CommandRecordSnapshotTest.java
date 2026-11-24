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
package com.anyilanxin.kunpeng.engine.bpmn.scheduling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.eventlog.serialize.BatchFrame;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.timer.TimerEventRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobKindType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobState;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
@DisplayName("批内命令快照透传")
class CommandRecordSnapshotTest {

  @Test
  @DisplayName("经批内快照写出的批帧与直接写源记录逐字节一致")
  void batchFrameIsByteIdenticalToWritingTheSourceRecordsDirectly() {
    final var batch = new HeapCommandBatch((count, bytes) -> true);
    // 入批后改写源记录：批内快照不受影响
    final var job = job();
    final var timer = timer();
    assertTrue(batch.append(1, -1, commandMetadata(JobLifeCycle.CREATING), job).isRight());
    assertTrue(batch.append(44, -1, commandMetadata(TimerLifeCycle.TRIGGER), timer).isRight());
    job.setVariables(Map.of("changed", true)).setJobType("rewritten");
    timer.setTimerId(999).setDueDate(1_900);

    final var direct =
        List.of(
            AppendEntry.of(1, commandMetadata(JobLifeCycle.CREATING), job()),
            AppendEntry.of(44, commandMetadata(TimerLifeCycle.TRIGGER), timer()));

    assertArrayEquals(frame(direct), frame(batch.entries()));
    assertEquals(
        direct.stream().mapToInt(AppendEntry::getLength).sum(), batch.byteSize());
  }

  @Test
  @DisplayName("探针拒绝的记录不入批，也不计入字节数")
  void rejectedRecordIsNotAppended() {
    final var batch = new HeapCommandBatch((count, bytes) -> count == 1);
    assertTrue(batch.append(1, -1, commandMetadata(JobLifeCycle.CREATING), job()).isRight());
    final int firstLength = batch.byteSize();

    final var rejected = batch.append(44, -1, commandMetadata(TimerLifeCycle.TRIGGER), timer());

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

  private static RecordMetadata commandMetadata(final ValueLifeCycle lifeCycle) {
    final var metadata = new RecordMetadata();
    metadata
        .valueLifeCycle(lifeCycle)
        .recordType(RecordType.COMMAND)
        .valueType(lifeCycle.getValueType())
        .recordVersion(1)
        .brokerVersion(LogEventWriter.BROKER_VERSION)
        .batchOperationReference(-1)
        .operationReference(-1)
        .requestId(-1);
    return metadata;
  }

  private static JobRecord job() {
    return new JobRecord()
        .setJobType("bench")
        .setJobKind(JobKindType.ACTIVITY)
        .setProcessDefinitionKey("bench")
        .setProcessDefinitionId(1)
        .setProcessInstanceId(7)
        .setState(JobState.ACTIVE)
        // 变量文档：字符串、整数、嵌套映射
        .setVariables(Map.of("k", "v", "n", 42L, "nested", Map.of("a", true)))
        .setLocalVariables(Map.of());
  }

  private static TimerEventRecord timer() {
    return new TimerEventRecord().setTimerId(44).setDueDate(900).setProcessDefinitionKey("test");
  }
}
