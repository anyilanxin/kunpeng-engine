/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.command.batch.processor;

import static com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.multiinstance.MultiInstanceConstant.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnLoopCharacteristics;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnMultiInstanceBody;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.MultiInstanceBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 并行多实例创建批执行器测试：每轮按 body 变量（计数器 + 集合快照）创建一个子实例（发子实例 ACTIVATING）、 游标自排、耗尽收尾；主体守卫（body 离开激活态即停）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class BatchMiCreateExecutorProcessorTest {
  private static final long REQUEST_ID = 300L;
  private static final long INSTANCE_ID = 100L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long PROCESS_DEFINITION_ID = 5L;
  private static final long BATCH_ID = 99L;

  private LogEventWriter writer;
  private VariableBehavior variableBehavior;
  private ActivityInstanceRecord instanceRecord;
  private ProcessInstanceBatchRecord batchValue;
  private BusinessLogRecord<ProcessInstanceBatchRecord> record;
  private BatchMiCreateExecutorProcessor processor;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final ImmutableBusinessRepository repository =
        mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    variableBehavior = mock(VariableBehavior.class);
    // 先构造后打桩：MultiInstanceBehavior 构造器会读仓储，混入 when() 会话即 UnfinishedStubbing
    final MultiInstanceBehavior multiInstanceBehavior =
        new MultiInstanceBehavior(writer, variableBehavior);
    final Behavior behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    // 执行器经 behavior.multiInstanceBehavior() 取展开行为：须在执行器构造前接线
    when(behavior.multiInstanceBehavior()).thenReturn(multiInstanceBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(writer.millis()).thenReturn(1L);
    when(writer.nextCurrentSourceKey(PROCESS_INSTANCE_ID)).thenReturn(101L, 102L, 103L);

    instanceRecord =
        new ActivityInstanceRecord()
            .setActivityInstanceId(INSTANCE_ID)
            .setLifeCycle(ActivityInstanceLifeCycle.ACTIVATED)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setRootProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setProcessDefinitionKey("p1")
            .setActivityDefinitionKey("mi1");
    when(repository.instanceRepository().getRecord(INSTANCE_ID)).thenReturn(instanceRecord);

    final BpmnMultiInstanceBody body =
        new BpmnMultiInstanceBody(
            "mi1", new BpmnLoopCharacteristics(false, null, null, null, "item"), new BpmnUserTask("inner"));
    final BpmnProcess process = new BpmnProcess("p1");
    process.addFlowElement(body);
    process.addFlowElement(new BpmnUserTask("inner"));
    when(repository.bpmnResourceRepository().getRuntime(PROCESS_DEFINITION_ID))
        .thenReturn(new ProcessDefinitionRuntime(process, null));

    batchValue =
        new ProcessInstanceBatchRecord()
            .setBatchId(BATCH_ID)
            .setRequestId(REQUEST_ID)
            .setBatchBusinessId(INSTANCE_ID)
            .setCurrentExecutionId(0);
    record = mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(BATCH_ID);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(batchValue);

    processor = new BatchMiCreateExecutorProcessor(writer);
  }

  private Map<String, Object> variables(
      final int active, final int instances, final int loopCounter, final List<String> collection) {
    final Map<String, Object> map = new HashMap<>();
    map.put(NR_OF_ACTIVE_INSTANCES, active);
    map.put(NR_OF_INSTANCES, instances);
    map.put(NR_OF_COMPLETED_INSTANCES, 0);
    map.put(NR_OF_TERMINATED_INSTANCES, 0);
    map.put(LOOP_COUNTER, loopCounter);
    map.put(INPUT_COLLECTION, collection);
    return map;
  }

  @Test
  void roundsCreateChildrenBySnapshotCursorThenFinish() {
    // 每轮读两次 body 变量（计数器 + 集合快照），计数器随轮推进（执行器写回经 mock 不落库，序列桩模拟逐轮状态）
    when(variableBehavior.getDirectVariable(any()))
        .thenReturn(variables(0, 3, -1, List.of("a", "b", "c")))
        .thenReturn(variables(0, 3, -1, List.of("a", "b", "c")))
        .thenReturn(variables(1, 3, 0, List.of("a", "b", "c")))
        .thenReturn(variables(1, 3, 0, List.of("a", "b", "c")))
        .thenReturn(variables(2, 3, 1, List.of("a", "b", "c")))
        .thenReturn(variables(2, 3, 1, List.of("a", "b", "c")));

    // 第 1 轮：建子实例（ACTIVATING），游标推进自排
    processor.processRecord(record);
    verify(writer)
        .addCommand(
            eq(101L), eq(ActivityInstanceLifeCycle.ACTIVATING), eq(REQUEST_ID), any());
    assertEquals(1, batchValue.getCurrentExecutionId());
    verify(writer)
        .addCommand(
            eq(BATCH_ID), eq(ProcessInstanceBatchState.ACTIVATE_MI_CREATE), eq(REQUEST_ID), any());

    // 第 2 轮：建第二个
    processor.processRecord(record);
    verify(writer)
        .addCommand(
            eq(102L), eq(ActivityInstanceLifeCycle.ACTIVATING), eq(REQUEST_ID), any());

    // 第 3 轮：建最后一个，名单耗尽收尾（批终态事件，不再自排）
    processor.processRecord(record);
    verify(writer)
        .addCommand(
            eq(103L), eq(ActivityInstanceLifeCycle.ACTIVATING), eq(REQUEST_ID), any());
    verify(writer)
        .addEvent(eq(BATCH_ID), eq(ProcessInstanceBatchState.BATCH_COMPLETED), eq(REQUEST_ID), any());
    // 3 轮中前 2 轮自排（第 3 轮耗尽收尾）
    verify(writer, times(2))
        .addCommand(
            eq(BATCH_ID), eq(ProcessInstanceBatchState.ACTIVATE_MI_CREATE), eq(REQUEST_ID), any());
  }

  @Test
  void cardinalityRoundCreatesChildWithoutSnapshot() {
    // 基数场景：body 变量无集合快照键，子实例照建（元素变量不落）
    final Map<String, Object> cardinality = new HashMap<>();
    cardinality.put(NR_OF_ACTIVE_INSTANCES, 0);
    cardinality.put(NR_OF_INSTANCES, 2);
    cardinality.put(NR_OF_COMPLETED_INSTANCES, 0);
    cardinality.put(NR_OF_TERMINATED_INSTANCES, 0);
    cardinality.put(LOOP_COUNTER, -1);
    when(variableBehavior.getDirectVariable(any())).thenReturn(cardinality);
    processor.processRecord(record);
    verify(writer)
        .addCommand(
            eq(101L), eq(ActivityInstanceLifeCycle.ACTIVATING), eq(REQUEST_ID), any());
    verify(writer)
        .addCommand(
            eq(BATCH_ID), eq(ProcessInstanceBatchState.ACTIVATE_MI_CREATE), eq(REQUEST_ID), any());
  }

  @Test
  void terminatingBodyStopsBatch() {
    // body 被终止（如边界中断/流程取消）：创建即停，发批终态事件收尾
    instanceRecord.setLifeCycle(ActivityInstanceLifeCycle.TERMINATING);
    processor.processRecord(record);
    verify(writer, never())
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), any());
    verify(writer)
        .addEvent(eq(BATCH_ID), eq(ProcessInstanceBatchState.BATCH_COMPLETED), eq(REQUEST_ID), any());
  }
}
