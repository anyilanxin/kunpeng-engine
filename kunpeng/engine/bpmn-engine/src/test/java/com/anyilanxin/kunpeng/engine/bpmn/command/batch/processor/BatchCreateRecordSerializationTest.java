/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can use redistribute it  and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.command.batch.processor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BatchBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.BatchBusinessType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 创建类批记录的 wire 可写性回归（2026-11-07 线上事故）：MI/加签创建批不设 batchAfter， 属性缺省必须让 structpack 可编码——mock
 * writer 的单测从不走序列化，真实 broker 的 addCommand copyValue 在未设值属性上抛 StructPackException 回滚整批。
 *
 * @author zxuanhong
 * @since 2026.11.1
 */
class BatchCreateRecordSerializationTest {
  private static final long REQUEST_ID = 300L;
  private static final long INSTANCE_ID = 100L;
  private static final long PROCESS_INSTANCE_ID = 9L;

  private LogEventWriter writer;
  private BatchBehavior batchBehavior;
  private ActivityContent content;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    when(writer.getRepository()).thenReturn(mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS));
    batchBehavior = new BatchBehavior(writer);
    final ActivityInstanceRecord instance =
        new ActivityInstanceRecord()
            .setActivityInstanceId(INSTANCE_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID);
    content = new ActivityContent(instance, REQUEST_ID);
  }

  @Test
  void createBatchesAreWireWritableWithoutBatchAfter() {
    batchBehavior.createMiCreateBatch(content);
    batchBehavior.createTaskCreateBatch(content);

    final ArgumentCaptor<UnifiedRecordValue> captor =
        ArgumentCaptor.forClass(UnifiedRecordValue.class);
    verify(writer, times(2))
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), captor.capture());
    for (final UnifiedRecordValue value : captor.getAllValues()) {
      final ProcessInstanceBatchRecord batch = (ProcessInstanceBatchRecord) value;
      assertDoesNotThrow(batch::getLength);
      // 无回调缺省必须是可解码组合：valueType=PROCESS_BATCH_INSTANCE + -1 经 from() default 返回全局 UNKNOWN
      // （未设置语义）；UNKNOW valueType 则会被 fromProtocolValue 直接拒绝，
      // sink 侧 Jackson 读 getBatchAfterLifeCycle 抛 IllegalStateException
      assertEquals(ValueType.PROCESS_BATCH_INSTANCE, batch.getBatchAfterValueType());
      assertDoesNotThrow(batch::getBatchAfterLifeCycle);
      assertEquals(ValueLifeCycle.UNKNOWN, batch.getBatchAfterLifeCycle());
      assertEquals(-1L, batch.getBatchAfterBusinessId());
      assertEquals(BatchBusinessType.UNKNOW, batch.getBatchAfterType());
    }
  }
}
