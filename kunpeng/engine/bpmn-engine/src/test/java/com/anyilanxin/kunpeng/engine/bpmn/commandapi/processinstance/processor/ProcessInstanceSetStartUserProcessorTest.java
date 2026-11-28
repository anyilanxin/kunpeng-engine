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
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.startuser.ProcessInstanceStartUserRequestRecord;
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
 * 修改流程发起人命令处理器：写发起人已修改事件（非命令，才落库与导出）并只改发起人；实例不存在 / 终态、新发起人为空三道校验； 授权未配置不校验操作人，启用后须管理员（发起人本人也不行）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class ProcessInstanceSetStartUserProcessorTest {
  private static final long REQUEST_ID = 700L;
  private static final long INSTANCE_ID = 1L;

  private LogEventWriter writer;
  private ProcessInstanceRecord originInstance;
  private ProcessInstanceSetStartUserProcessor processor;

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
    processor = new ProcessInstanceSetStartUserProcessor(writer);
  }

  private void processRecord(final String startUserId) {
    final ProcessInstanceStartUserRequestRecord value = new ProcessInstanceStartUserRequestRecord();
    value.setStartUserId(startUserId);
    @SuppressWarnings("unchecked")
    final BusinessLogRecord<ProcessInstanceStartUserRequestRecord> record =
        mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(INSTANCE_ID);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    when(record.getValue()).thenReturn(value);
    processor.processRecord(record);
  }

  private String capturedStartUser() {
    final ArgumentCaptor<ProcessInstanceRecord> instance =
        ArgumentCaptor.forClass(ProcessInstanceRecord.class);
    // 取最后一次：同一用例内多次设置时断言最新值
    verify(writer, atLeastOnce())
        .addEvent(
            eq(INSTANCE_ID), eq(ProcessInstanceLifeCycle.START_USER), eq(REQUEST_ID), instance.capture());
    return instance.getValue().getStartUserId();
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
  void writesStartUserEventAndResponds() {
    // 未接入授权体系：容器无 BrokerCfg = 功能关闭，不校验操作人
    processRecord("bob");

    assertEquals("bob", capturedStartUser());
    assertEquals("", originInstance.getDelegateUserId(), "只改发起人，不动代办人");
    verify(writer)
        .adResponse(
            eq(CommandApiProcessInstanceValueLifeCycle.SET_START_USER_RESPONSE),
            eq(REQUEST_ID),
            any());
    verify(writer, never()).adErrorResponse(anyLong(), anyInt(), anyString());
    // 写的是事件（落库 + 导出），不是命令：命令只进日志，由命令处理器决定是否落库
    verify(writer, never())
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void rejectsBlankStartUser() {
    processRecord("");

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(0), contains("发起人不能为空"));
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void rejectsMissingInstance() {
    when(writer.getRepository().processInstanceRepository().getRecord(INSTANCE_ID))
        .thenReturn(null);

    processRecord("bob");

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(404), contains("流程实例不存在"));
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void rejectsTerminalInstance() {
    originInstance.setState(ProcessInstanceState.TERMINATING);

    processRecord("bob");

    verify(writer).adErrorResponse(eq(REQUEST_ID), eq(404), contains("无法修改发起人"));
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }

  @Test
  void enabledAuthorizationRequiresAdmin() {
    enableAuthorization(List.of("admins"));
    when(writer.currentAuthorizationMetadata()).thenReturn("{\"userId\":\"mallory\"}");

    processRecord("bob");
    verify(writer)
        .adErrorResponse(eq(REQUEST_ID), eq(AuthorizationDeniedException.CODE), anyString());
    verify(writer, never())
        .addEvent(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));

    // 发起人本人也不能过户自己的实例
    when(writer.currentAuthorizationMetadata()).thenReturn("{\"userId\":\"alice\"}");
    processRecord("bob");
    verify(writer, atLeastOnce())
        .adErrorResponse(eq(REQUEST_ID), eq(AuthorizationDeniedException.CODE), anyString());

    // 管理员：离职交接
    when(writer.currentAuthorizationMetadata())
        .thenReturn("{\"userId\":\"root\",\"groups\":[\"admins\"]}");
    processRecord("bob");
    assertEquals("bob", capturedStartUser());
  }
}
