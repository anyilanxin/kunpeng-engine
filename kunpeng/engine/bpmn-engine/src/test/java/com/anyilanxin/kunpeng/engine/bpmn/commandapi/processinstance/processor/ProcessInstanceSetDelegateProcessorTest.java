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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.processinstance.processor;

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
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.delegateuser.ProcessInstanceDelegateUserRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceState;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.CommandApiProcessInstanceValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 设置流程实例代办人命令处理器：写代办人已设置事件（非命令，才落库与导出）并只改代办人；实例不存在 / 终态、实例无发起人、代办人=发起人四道校验； 授权未配置不校验操作人，启用后须发起人本人或管理员。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class ProcessInstanceSetDelegateProcessorTest {
  private static final long REQUEST_ID = 800L;
  private static final long INSTANCE_ID = 1L;

  private LogEventWriter writer;
  private ProcessInstanceRecord originInstance;
  private ProcessInstanceSetDelegateProcessor processor;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final ImmutableBusinessRepository repository =
        mock(ImmutableBusinessRepository.class, RETURNS_DEEP_STUBS);
    originInstance =
        new ProcessInstanceRecord()
            .setProcessInstanceId(INSTANCE_ID)
            .setState(ProcessInstanceState.ACTIVATED)
            .setStartUserId("alice");
    when(repository.processInstanceRepository().getRecord(INSTANCE_ID)).thenReturn(originInstance);
    when(writer.getRepository()).thenReturn(repository);
    processor = new ProcessInstanceSetDelegateProcessor(writer);
  }

  private void processRecord(final String delegateUserId) {
    final ProcessInstanceDelegateUserRequestRecord value =
        new ProcessInstanceDelegateUserRequestRecord();
    value.setDelegateUserId(delegateUserId);
    @SuppressWarnings("unchecked")
    final BusinessLogRecord<ProcessInstanceDelegateUserRequestRecord> record =
        mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(INSTANCE_ID);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(value);
    processor.processRecord(record);
  }

  private String capturedDelegate() {
    final ArgumentCaptor<ProcessInstanceRecord> instance =
        ArgumentCaptor.forClass(ProcessInstanceRecord.class);
    // 取最后一次：同一用例内多次设置时断言最新值
    verify(writer, atLeastOnce())
        .addEvent(
            eq(INSTANCE_ID),
            eq(ProcessInstanceLifeCycle.DELEGATE_USER),
            eq(REQUEST_ID),
            instance.capture());
    return instance.getValue().getDelegateUserId();
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
    // 未接入授权体系：容器无 BrokerCfg = 功能关闭，不校验操作人
    processRecord("carol");

    assertEquals("carol", capturedDelegate());
    assertEquals("alice", originInstance.getStartUserId(), "只改代办人，不改发起人");
    verify(writer)
        .adResponse(
            eq(CommandApiProcessInstanceValueLifeCycle.SET_DELEGATE_USER_RESPONSE),
            eq(REQUEST_ID),
            any());
    verify(writer, never()).adErrorResponse(anyLong(), anyInt(), anyString());
    // 写的是事件（落库 + 导出），不是命令：命令只进日志，由命令处理器决定是否落库
    verify(writer, never())
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void emptyDelegateClears() {
    originInstance.setDelegateUserId("carol");

    processRecord("");

    assertEquals("", capturedDelegate());
  }

  @Test
  void rejectsMissingInstance() {
    when(writer.getRepository().processInstanceRepository().getRecord(INSTANCE_ID))
        .thenReturn(null);

    processRecord("carol");

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(404), contains("流程实例不存在"));
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void rejectsTerminalInstance() {
    originInstance.setState(ProcessInstanceState.CANCELED);

    processRecord("carol");

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(404), contains("无法设置代办人"));
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void rejectsInstanceWithoutStartUser() {
    // 无发起人信息的实例无代办可言
    final ProcessInstanceRecord legacyInstance =
        new ProcessInstanceRecord()
            .setProcessInstanceId(INSTANCE_ID)
            .setState(ProcessInstanceState.ACTIVATED);
    when(writer.getRepository().processInstanceRepository().getRecord(INSTANCE_ID))
        .thenReturn(legacyInstance);

    processRecord("carol");

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(0), contains("流程实例无发起人信息"));
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void rejectsDelegateEqualToStartUser() {
    processRecord("alice");

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(0), contains("代办人不能是发起人本人"));
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void enabledAuthorizationRequiresStartUserOrAdmin() {
    enableAuthorization(List.of("admins"));
    when(writer.currentAuthorizationMetadata()).thenReturn("{\"userId\":\"mallory\"}");

    processRecord("carol");
    verify(writer)
        .adErrorResponse(eq(REQUEST_ID), eq(AuthorizationDeniedException.CODE), anyString());
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));

    // 发起人本人：给自己找代办
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
