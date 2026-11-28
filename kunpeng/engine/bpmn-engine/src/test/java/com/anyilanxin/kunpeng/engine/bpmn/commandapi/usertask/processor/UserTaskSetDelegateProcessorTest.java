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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.authorization.AuthorizationDeniedException;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.delegateuser.UserTaskDelegateUserRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 设置代办人命令处理器：写代办人已设置事件（非命令，才落库与导出）并只改代办人；任务状态、未指定审批人、代办人=审批人三道校验； 授权未配置不校验操作人，启用后须审批人本人或管理员。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class UserTaskSetDelegateProcessorTest {
  private static final long REQUEST_ID = 400L;
  private static final long TASK_ID = 1L;

  private LogEventWriter writer;
  private UserTaskRecord originTask;
  private UserTaskSetDelegateProcessor processor;

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
            .setAssignee("alice");
    when(repository.userTaskRepository().getRecord(TASK_ID)).thenReturn(originTask);
    when(writer.getRepository()).thenReturn(repository);
    processor = new UserTaskSetDelegateProcessor(writer);
  }

  private void processRecord(final String delegateUserId) {
    final UserTaskDelegateUserRequestRecord value = new UserTaskDelegateUserRequestRecord();
    value.setDelegateUserId(delegateUserId);
    @SuppressWarnings("unchecked")
    final BusinessLogRecord<UserTaskDelegateUserRequestRecord> record =
        mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(TASK_ID);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(value);
    processor.processRecord(record);
  }

  private String capturedDelegate() {
    final ArgumentCaptor<UserTaskRecord> task = ArgumentCaptor.forClass(UserTaskRecord.class);
    // 取最后一次：同一用例内多次设置时断言最新值
    verify(writer, atLeastOnce())
        .addEvent(eq(TASK_ID), eq(UserTaskLifeCycle.DELEGATE_USER), eq(REQUEST_ID), task.capture());
    return task.getValue().getDelegateUserId();
  }

  private void enableAuthorization(final List<String> adminGroups) {
    final BrokerCfg brokerCfg = new BrokerCfg();
    brokerCfg.getAuthorization().setEnabled(true);
    brokerCfg.getAuthorization().setAdminGroups(adminGroups);
    @SuppressWarnings("unchecked")
    final ObjectProvider<BrokerCfg> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(brokerCfg);
    final BeanFactory beanFactory = mock(BeanFactory.class);
    when(beanFactory.getBeanProvider(BrokerCfg.class)).thenReturn(provider);
    when(writer.getBeanFactory()).thenReturn(beanFactory);
  }

  @Test
  void writesDelegateUserEventAndResponds() {
    processRecord("carol");

    assertEquals("carol", capturedDelegate());
    assertEquals("alice", originTask.getAssignee(), "只改代办人，不改审批人");
    verify(writer)
        .adResponse(
            eq(CommandApiUserTaskValueLifeCycle.SET_DELEGATE_USER_RESPONSE), eq(REQUEST_ID), any());
    verify(writer, never()).adErrorResponse(anyLong(), anyInt(), anyString());
    // 写的是事件（落库 + 导出），不是命令：命令只进日志，状态处理器为空就不落库
    verify(writer, never())
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void emptyDelegateClears() {
    originTask.setDelegateUserId("carol");

    processRecord("");

    assertEquals("", capturedDelegate());
  }

  @Test
  void rejectsWhenTaskNotCreated() {
    originTask.setLifeCycle(UserTaskLifeCycle.COMPLETING);

    processRecord("carol");

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(0), contains("当前任务状态不允许此操作"));
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void rejectsTaskWithoutAssignee() {
    // 未指定审批人的候选任务无代办可言
    final UserTaskRecord candidateTask =
        new UserTaskRecord()
            .setTaskId(TASK_ID)
            .setLifeCycle(UserTaskLifeCycle.CREATED)
            .setState(UserTaskState.ACTIVE);
    when(writer.getRepository().userTaskRepository().getRecord(TASK_ID))
        .thenReturn(candidateTask);

    processRecord("carol");

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(0), contains("任务未指定审批人"));
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void rejectsDelegateEqualToAssignee() {
    processRecord("alice");

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(0), contains("代办人不能是审批人本人"));
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void withoutAuthorizationConfigAnyoneMaySetDelegate() {
    // 未接入授权体系：容器无 BrokerCfg = 功能关闭，不校验操作人
    processRecord("carol");

    assertEquals("carol", capturedDelegate());
    verify(writer)
        .adResponse(
            eq(CommandApiUserTaskValueLifeCycle.SET_DELEGATE_USER_RESPONSE), eq(REQUEST_ID), any());
  }

  @Test
  void enabledAuthorizationRequiresAssigneeOrAdmin() {
    enableAuthorization(List.of("admins"));
    when(writer.currentAuthorizationMetadata()).thenReturn("{\"userId\":\"mallory\"}");

    processRecord("carol");
    verify(writer)
        .adErrorResponse(eq(REQUEST_ID), eq(AuthorizationDeniedException.CODE), anyString());
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));

    // 审批人本人：给自己找代办
    when(writer.currentAuthorizationMetadata()).thenReturn("{\"userId\":\"alice\"}");
    processRecord("carol");
    assertEquals("carol", capturedDelegate());

    // 管理员：离职交接场景由管理员逐个指定
    when(writer.currentAuthorizationMetadata())
        .thenReturn("{\"userId\":\"root\",\"groups\":[\"admins\"]}");
    processRecord("dave");
    assertEquals("dave", capturedDelegate());
  }
}
