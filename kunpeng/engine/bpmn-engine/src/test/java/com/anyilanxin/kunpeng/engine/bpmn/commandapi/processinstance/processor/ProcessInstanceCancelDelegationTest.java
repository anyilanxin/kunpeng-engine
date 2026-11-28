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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
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
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.cancel.ProcessInstanceCancelRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceState;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 取消流程实例时的代办人 id：名单代办与实例代办人记操作人，发起人本人取消清空（含此前用命令指定的代办人），无身份与管理员旁路不动； 判定不跟随授权开关（用例默认无授权配置）；启用授权后非发起人非代办人被拒、不发取消命令。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class ProcessInstanceCancelDelegationTest {
  private static final long REQUEST_ID = 600L;
  private static final long INSTANCE_ID = 1L;

  private LogEventWriter writer;
  private ProcessInstanceRecord originInstance;
  private ProcessInstanceCancelProcessor cancelProcessor;

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
    cancelProcessor = new ProcessInstanceCancelProcessor(writer);
  }

  /** 取消实例；断言取消命令照常下发、无错误响应，代办人 id 断言在取消命令携带的那条实例记录上 */
  private void cancel() {
    @SuppressWarnings("unchecked")
    final BusinessLogRecord<ProcessInstanceCancelRequestRecord> record =
        mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(INSTANCE_ID);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    cancelProcessor.processRecord(record);
    verify(writer)
        .addCommand(eq(INSTANCE_ID), eq(ProcessInstanceLifeCycle.CANCEL), eq(REQUEST_ID), any());
    verify(writer, never()).adErrorResponse(anyLong(), anyInt(), anyString());
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
  void actForCancelRecordsOperator() {
    // 授权未配置（关闭）同样记录：判定不跟随授权开关
    when(writer.currentAuthorizationMetadata())
        .thenReturn("{\"userId\":\"carol\",\"actFor\":[\"alice\"]}");

    cancel();
    assertEquals("carol", originInstance.getDelegateUserId());
  }

  @Test
  void selfCancelClearsDelegateSetByCommand() {
    // 发起人本人取消：即使此前用命令指定过代办人也清空
    originInstance.setDelegateUserId("carol");
    when(writer.currentAuthorizationMetadata()).thenReturn("{\"userId\":\"alice\"}");

    cancel();
    assertEquals("", originInstance.getDelegateUserId());
  }

  @Test
  void instanceDelegateCancelRecordsOperator() {
    // 命令路径指定 + 带身份取消：实例上的代办人就是操作人
    originInstance.setDelegateUserId("carol");
    when(writer.currentAuthorizationMetadata()).thenReturn("{\"userId\":\"carol\"}");

    cancel();
    assertEquals("carol", originInstance.getDelegateUserId());
  }

  @Test
  void withoutIdentityKeepsCommandDelegate() {
    // 未接入授权体系：引擎分不清是谁取消的，以命令写入的为准
    originInstance.setDelegateUserId("carol");
    when(writer.currentAuthorizationMetadata()).thenReturn("");

    cancel();
    assertEquals("carol", originInstance.getDelegateUserId());
  }

  @Test
  void adminCancelLeavesDelegateUntouched() {
    enableAuthorization(List.of("admins"));
    originInstance.setDelegateUserId("carol");
    when(writer.currentAuthorizationMetadata())
        .thenReturn("{\"userId\":\"root\",\"groups\":[\"admins\"]}");

    cancel();
    assertEquals("carol", originInstance.getDelegateUserId());
  }

  @Test
  void outsiderIsDeniedWhenAuthorizationEnabled() {
    enableAuthorization(List.of("admins"));
    when(writer.currentAuthorizationMetadata()).thenReturn("{\"userId\":\"mallory\"}");

    @SuppressWarnings("unchecked")
    final BusinessLogRecord<ProcessInstanceCancelRequestRecord> record =
        mock(BusinessLogRecord.class);
    when(record.getKey()).thenReturn(INSTANCE_ID);
    when(record.getRequestId()).thenReturn(REQUEST_ID);
    cancelProcessor.processRecord(record);

    verify(writer)
        .adErrorResponse(eq(REQUEST_ID), eq(AuthorizationDeniedException.CODE), anyString());
    verify(writer, never())
        .addCommand(anyLong(), any(ValueLifeCycle.class), anyLong(), any(UnifiedRecordValue.class));
  }
}
