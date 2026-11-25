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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.ActivityInstanceBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.IncidentBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.JobBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.processor.BpmnProcessElementCanceledProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.processor.BpmnProcessElementTerminatedProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.processor.BpmnUserTaskCanceledProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.processor.BpmnUserTaskTerminatedProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.incodent.IncidentRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 属主（活动实例 / 流程实例 / 用户任务）到终态时同步删除其挂着的 incident 的测试：五条终态路径各写一条 incident
 * {@code DELETED} 事件，无在途 incident 时空操作。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
class IncidentOwnerTerminalCleanupTest {
  private static final long REQUEST_ID = 300L;
  private static final long TASK_ID = 1L;
  private static final long INSTANCE_ID = 100L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long INCIDENT_ID = 777L;

  private LogEventWriter writer;
  private ImmutableBusinessRepository repository;
  private IncidentBehavior incidentBehavior;
  private IncidentRecord ownedIncident;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    repository = mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    when(writer.getRepository()).thenReturn(repository);
    final Behavior behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    // 先构造后打桩：IncidentBehavior 构造器会读仓储，混入 when() 会话即 UnfinishedStubbing
    incidentBehavior = new IncidentBehavior(writer);
    when(behavior.incidentBehavior()).thenReturn(incidentBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(writer.millis()).thenReturn(1L);
    // 任务处理器经基类 processRecord 按定义 id + 元素 key 取真实元素
    final BpmnProcess process = new BpmnProcess("p1");
    process.addFlowElement(new BpmnUserTask("task1"));
    when(repository.bpmnResourceRepository().getRuntime(5L))
        .thenReturn(new ProcessDefinitionRuntime(process, null));
    ownedIncident =
        new IncidentRecord()
            .setIncidentId(INCIDENT_ID)
            .setTaskId(TASK_ID)
            .setActivityInstanceId(INSTANCE_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID);
  }

  /** 任务终止：其挂着的 incident 写 DELETED */
  @Test
  void terminatedUserTaskDeletesOwnedIncident() {
    when(repository.incidentRepository().getRecordByBusinessId(TASK_ID))
        .thenReturn(ownedIncident);
    final UserTaskRecord task = taskRecord();

    new BpmnUserTaskTerminatedProcessor(writer).processRecord(logRecord(task));

    verify(writer).addEvent(TASK_ID, UserTaskLifeCycle.TERMINATED, REQUEST_ID, task);
    verify(writer).addEvent(INCIDENT_ID, IncidentLifeCycle.DELETED, REQUEST_ID, ownedIncident);
  }

  /** 任务取消：其挂着的 incident 写 DELETED */
  @Test
  void canceledUserTaskDeletesOwnedIncident() {
    when(repository.incidentRepository().getRecordByBusinessId(TASK_ID))
        .thenReturn(ownedIncident);
    when(repository.instanceRepository().getRecord(INSTANCE_ID))
        .thenReturn(new ActivityInstanceRecord().setActivityInstanceId(INSTANCE_ID));
    final UserTaskRecord task = taskRecord();

    new BpmnUserTaskCanceledProcessor(writer).processRecord(logRecord(task));

    verify(writer).addEvent(TASK_ID, UserTaskLifeCycle.CANCELED, REQUEST_ID, task);
    verify(writer).addEvent(INCIDENT_ID, IncidentLifeCycle.DELETED, REQUEST_ID, ownedIncident);
  }

  /** 流程实例终止：实例级 incident 写 DELETED */
  @Test
  void terminatedProcessInstanceDeletesOwnedIncident() {
    when(repository.incidentRepository().getRecordByBusinessId(PROCESS_INSTANCE_ID))
        .thenReturn(ownedIncident);
    final ProcessInstanceRecord instance = instanceRecord();

    new BpmnProcessElementTerminatedProcessor(writer).processRecord(logRecord(instance));

    verify(writer)
        .addEvent(PROCESS_INSTANCE_ID, ProcessInstanceLifeCycle.TERMINATED, REQUEST_ID, instance);
    verify(writer).addEvent(INCIDENT_ID, IncidentLifeCycle.DELETED, REQUEST_ID, ownedIncident);
  }

  /** 流程实例取消：实例级 incident 写 DELETED */
  @Test
  void canceledProcessInstanceDeletesOwnedIncident() {
    when(repository.incidentRepository().getRecordByBusinessId(PROCESS_INSTANCE_ID))
        .thenReturn(ownedIncident);
    final ProcessInstanceRecord instance = instanceRecord();

    new BpmnProcessElementCanceledProcessor(writer).processRecord(logRecord(instance));

    verify(writer)
        .addEvent(PROCESS_INSTANCE_ID, ProcessInstanceLifeCycle.CANCELED, REQUEST_ID, instance);
    verify(writer).addEvent(INCIDENT_ID, IncidentLifeCycle.DELETED, REQUEST_ID, ownedIncident);
  }

  /** 活动终止开始（onTerminating，与 job 取消同点）：属主活动的 incident 写 DELETED */
  @Test
  void terminatingActivityDeletesOwnedIncident() {
    when(repository.incidentRepository().getRecordByBusinessId(INSTANCE_ID))
        .thenReturn(ownedIncident);
    final ActivityInstanceRecord instance =
        new ActivityInstanceRecord().setActivityInstanceId(INSTANCE_ID);
    final ActivityContent content =
        new ActivityContent(instance, REQUEST_ID, 7L, 8L);
    final ActivityInstanceBehavior behavior =
        new ActivityInstanceBehavior(mock(JobBehavior.class), writer, incidentBehavior);

    behavior.onTerminating(content);

    verify(writer).addEvent(INCIDENT_ID, IncidentLifeCycle.DELETED, REQUEST_ID, ownedIncident);
    verify(writer)
        .addEvent(INSTANCE_ID, ActivityInstanceLifeCycle.TERMINATING, REQUEST_ID, instance);
  }

  /** 属主没有在途 incident：不写 DELETED（deep stub 默认返回 mock，显式打桩为 null） */
  @Test
  void missingOwnedIncidentIsNoOp() {
    when(repository.incidentRepository().getRecordByBusinessId(anyLong())).thenReturn(null);
    new BpmnUserTaskTerminatedProcessor(writer).processRecord(logRecord(taskRecord()));

    verify(writer, never())
        .addEvent(anyLong(), eq(IncidentLifeCycle.DELETED), anyLong(), any(UnifiedRecordValue.class));
  }

  private UserTaskRecord taskRecord() {
    return new UserTaskRecord()
        .setTaskId(TASK_ID)
        .setActivityInstanceId(INSTANCE_ID)
        .setProcessInstanceId(PROCESS_INSTANCE_ID)
        .setProcessDefinitionId(5L)
        .setTaskDefinitionKey("task1");
  }

  private ProcessInstanceRecord instanceRecord() {
    return new ProcessInstanceRecord()
        .setProcessInstanceId(PROCESS_INSTANCE_ID)
        .setProcessDefinitionId(5L)
        .setProcessDefinitionKey("p1");
  }

  private <T extends UnifiedRecordValue> BusinessLogRecord<T> logRecord(final T value) {
    final BusinessLogRecord<T> record = mock(BusinessLogRecord.class);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(value);
    return record;
  }
}
