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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.complete.UserTaskCompleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 完成任务时的代办人 id：名单代办与任务代办人记操作人，本人自办清空（含此前用命令指定的代办人），无身份与管理员旁路不动； 判定不跟随授权开关（用例默认无授权配置）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class UserTaskCompleteDelegationTest {
  private static final long REQUEST_ID = 500L;
  private static final long TASK_ID = 1L;
  private static final long INSTANCE_ID = 100L;
  private static final long PROCESS_INSTANCE_ID = 9L;
  private static final long PROCESS_DEFINITION_ID = 5L;

  private LogEventWriter writer;
  private UserTaskRecord originTask;
  private UserTaskCompleteProcessor completeProcessor;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final ImmutableBusinessRepository repository =
        mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    originTask =
        new UserTaskRecord()
            .setTaskId(TASK_ID)
            .setLifeCycle(UserTaskLifeCycle.CREATED)
            .setState(UserTaskState.ACTIVE)
            .setAssignee("alice")
            .setActivityInstanceId(INSTANCE_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setTaskDefinitionKey("task1");
    final ActivityInstanceRecord instanceRecord =
        new ActivityInstanceRecord()
            .setActivityInstanceId(INSTANCE_ID)
            .setTaskId(TASK_ID)
            .setProcessInstanceId(PROCESS_INSTANCE_ID)
            .setRootProcessInstanceId(PROCESS_INSTANCE_ID)
            .setProcessDefinitionId(PROCESS_DEFINITION_ID)
            .setProcessDefinitionKey("p1")
            .setActivityDefinitionKey("task1")
            .setActivityDefinitionType(BpmnElementType.USER_TASK)
            .setAddSignState(AddSignState.NONE);
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(originTask);
    when(repository.instanceRepository().getRecord(INSTANCE_ID)).thenReturn(instanceRecord);
    when(writer.getRepository()).thenReturn(repository);
    final VariableBehavior variableBehavior = mock(VariableBehavior.class);
    // 先构造再打桩：构造器内调用 mock 方法会撞上未完成的 stubbing
    final AddSignBehavior addSignBehavior =
        new AddSignBehavior(writer, mock(VariableBehavior.class));
    final Behavior behavior = mock(Behavior.class, RETURNS_DEEP_STUBS);
    when(behavior.addSignBehavior()).thenReturn(addSignBehavior);
    when(behavior.variableBehavior()).thenReturn(variableBehavior);
    when(writer.behavior()).thenReturn(behavior);
    when(writer.millis()).thenReturn(1L);
    when(writer.getSourceId()).thenReturn(0);
    completeProcessor = new UserTaskCompleteProcessor(writer);
  }

  /** 完成任务；断言完成链照常推进（COMPLETING 命令、无错误响应），返回值即命令携带的那条任务记录 */
  private UserTaskRecord complete() {
    final UserTaskCompleteRequestRecord value = new UserTaskCompleteRequestRecord();
    value.setTaskId(TASK_ID);
    value.setVariables(Map.of());
    @SuppressWarnings("unchecked")
    final BusinessLogRecord<UserTaskCompleteRequestRecord> record =
        mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(TASK_ID);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(value);
    completeProcessor.processRecord(record);
    verify(writer)
        .addCommand(eq(TASK_ID), eq(UserTaskLifeCycle.COMPLETING), eq(REQUEST_ID), any());
    verify(writer, never()).adErrorResponse(anyLong(), anyInt(), anyString());
    return originTask;
  }

  @Test
  void actForCompletionRecordsOperator() {
    // 授权未配置（关闭）同样记录：判定不跟随授权开关
    when(writer.currentAuthorizationMetadata())
        .thenReturn("{\"userId\":\"carol\",\"actFor\":[\"alice\"]}");

    assertEquals("carol", complete().getDelegateUserId());
  }

  @Test
  void selfCompletionClearsDelegateSetByCommand() {
    // 本人自己办完：即使此前用命令指定过代办人也清空，代办人的已办里不该出现这条
    originTask.setDelegateUserId("carol");
    when(writer.currentAuthorizationMetadata()).thenReturn("{\"userId\":\"alice\"}");

    assertEquals("", complete().getDelegateUserId());
  }

  @Test
  void taskDelegateCompletionRecordsOperator() {
    // 命令路径指定 + 带身份完成：任务上的代办人就是操作人
    originTask.setDelegateUserId("carol");
    when(writer.currentAuthorizationMetadata()).thenReturn("{\"userId\":\"carol\"}");

    assertEquals("carol", complete().getDelegateUserId());
  }

  @Test
  void withoutIdentityKeepsCommandDelegate() {
    // 未接入授权体系：引擎分不清是谁完成的，以命令写入的为准
    originTask.setDelegateUserId("carol");
    when(writer.currentAuthorizationMetadata()).thenReturn("");

    assertEquals("carol", complete().getDelegateUserId());
  }

  @Test
  void adminCompletionLeavesDelegateUntouched() {
    final BrokerCfg brokerCfg = new BrokerCfg();
    brokerCfg.getAuthorization().setEnabled(true);
    brokerCfg.getAuthorization().setAdminGroups(List.of("admins"));
    @SuppressWarnings("unchecked")
    final ObjectProvider<BrokerCfg> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(brokerCfg);
    final BeanFactory beanFactory = mock(BeanFactory.class);
    when(beanFactory.getBeanProvider(BrokerCfg.class)).thenReturn(provider);
    when(writer.getBeanFactory()).thenReturn(beanFactory);
    originTask.setDelegateUserId("carol");
    when(writer.currentAuthorizationMetadata())
        .thenReturn("{\"userId\":\"root\",\"groups\":[\"admins\"]}");

    assertEquals("carol", complete().getDelegateUserId());
  }
}
