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
package com.anyilanxin.kunpeng.engine.bpmn.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.configuration.broker.AuthorizationCfg;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 引擎侧授权与代为办理校验：完成任务与取消流程实例的三种放行（属主本人 / 代办名单含属主 / 属主身上的代办人）、完成与取消时代办人 id 的处理、
 * 设置代办人与修改发起人的权限，以及各自与授权开关的关系。属主 = 任务审批人 / 流程发起人。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class AuthorizationHandlerTest {
  private LogEventWriter writer;
  private AuthorizationCfg cfg;
  private AuthorizationHandler handler;

  @BeforeEach
  void setUp() {
    writer = mock(LogEventWriter.class);
    final BrokerCfg brokerCfg = new BrokerCfg();
    cfg = brokerCfg.getAuthorization();
    @SuppressWarnings("unchecked")
    final ObjectProvider<BrokerCfg> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(brokerCfg);
    final BeanFactory beanFactory = mock(BeanFactory.class);
    when(beanFactory.getBeanProvider(BrokerCfg.class)).thenReturn(provider);
    when(writer.getBeanFactory()).thenReturn(beanFactory);
    handler = new AuthorizationHandler(writer);
  }

  /** 授权元数据 JSON：groups / actFor 逐项加引号拼成 JSON 数组 */
  private void operator(
      final String userId, final List<String> actFor, final List<String> groups) {
    when(writer.currentAuthorizationMetadata())
        .thenReturn(
            "{\"userId\":\""
                + userId
                + "\",\"groups\":"
                + jsonArray(groups)
                + ",\"actFor\":"
                + jsonArray(actFor)
                + "}");
  }

  private static String jsonArray(final List<String> values) {
    return values.stream().map(v -> "\"" + v + "\"").collect(Collectors.joining(",", "[", "]"));
  }

  /** assignee 传 null 即未指定审批人的候选任务（setAssignee(null) 不写，保持缺省空串） */
  private static UserTaskRecord task(final String assignee, final String delegateUserId) {
    final UserTaskRecord record = new UserTaskRecord();
    record.setAssignee(assignee);
    record.setDelegateUserId(delegateUserId);
    return record;
  }

  @Test
  void assigneeHimselfCompletes() {
    cfg.setEnabled(true);
    operator("alice", List.of(), List.of());
    assertDoesNotThrow(() -> handler.checkCompleteTask(task("alice", "")));
  }

  @Test
  void delegateInActForListCompletes() {
    cfg.setEnabled(true);
    operator("carol", List.of("alice"), List.of());
    assertDoesNotThrow(() -> handler.checkCompleteTask(task("alice", "")));
  }

  @Test
  void delegateOnTaskCompletes() {
    cfg.setEnabled(true);
    operator("carol", List.of(), List.of());
    assertDoesNotThrow(() -> handler.checkCompleteTask(task("alice", "carol")));
  }

  @Test
  void outsiderIsDenied() {
    cfg.setEnabled(true);
    operator("mallory", List.of("bob"), List.of());
    assertThrows(
        AuthorizationDeniedException.class, () -> handler.checkCompleteTask(task("alice", "")));
  }

  @Test
  void adminBypasses() {
    cfg.setEnabled(true);
    cfg.setAdminGroups(List.of("admins"));
    operator("root", List.of(), List.of("admins"));
    assertDoesNotThrow(() -> handler.checkCompleteTask(task("alice", "")));
  }

  @Test
  void emptyUserIdIsNotMistakenForTaskDelegate() {
    // 任务未指定代办人（空串）时，空 userId 的操作人不能被当成"任务上的代办人"
    cfg.setEnabled(true);
    operator("", List.of(), List.of());
    assertThrows(
        AuthorizationDeniedException.class, () -> handler.checkCompleteTask(task("alice", "")));
  }

  @Test
  void disabledAuthorizationPassesWithoutOperator() {
    cfg.setEnabled(false);
    when(writer.currentAuthorizationMetadata()).thenReturn(null);
    assertDoesNotThrow(() -> handler.checkCompleteTask(task("alice", "")));
  }

  @Test
  void selfCompletionClearsDelegate() {
    // 授权关闭同样判定：本人自办清空此前用命令指定的代办人
    operator("alice", List.of(), List.of());
    assertEquals("", handler.delegateOnComplete(task("alice", "carol")));
  }

  @Test
  void actForCompletionRecordsOperator() {
    operator("carol", List.of("alice"), List.of());
    assertEquals("carol", handler.delegateOnComplete(task("alice", "")));
  }

  @Test
  void taskDelegateCompletionRecordsOperator() {
    operator("carol", List.of(), List.of());
    assertEquals("carol", handler.delegateOnComplete(task("alice", "carol")));
  }

  @Test
  void adminCompletionLeavesDelegateUntouched() {
    cfg.setEnabled(true);
    cfg.setAdminGroups(List.of("admins"));
    operator("root", List.of(), List.of("admins"));
    assertNull(handler.delegateOnComplete(task("alice", "carol")));
  }

  @Test
  void withoutIdentityDelegateIsLeftToCommand() {
    when(writer.currentAuthorizationMetadata()).thenReturn("");
    assertNull(handler.delegateOnComplete(task("alice", "carol")));
  }

  @Test
  void candidateTaskHasNoDelegate() {
    operator("carol", List.of("alice"), List.of());
    assertNull(handler.delegateOnComplete(task(null, "")));
  }

  @Test
  void assigneeOrAdminMaySetDelegate() {
    cfg.setEnabled(true);
    cfg.setAdminGroups(List.of("admins"));
    operator("alice", List.of(), List.of());
    assertDoesNotThrow(() -> handler.checkSetDelegate(task("alice", "")));
    operator("root", List.of(), List.of("admins"));
    assertDoesNotThrow(() -> handler.checkSetDelegate(task("alice", "")));
  }

  @Test
  void othersMayNotSetDelegate() {
    // 名单里的代办人只代办办理，不能改任务的代办人
    cfg.setEnabled(true);
    operator("carol", List.of("alice"), List.of());
    assertThrows(AuthorizationDeniedException.class, () -> handler.checkSetDelegate(task("alice", "")));
  }

  @Test
  void disabledAuthorizationLetsAnyoneSetDelegate() {
    cfg.setEnabled(false);
    assertDoesNotThrow(() -> handler.checkSetDelegate(task("alice", "")));
  }

  /** startUserId 传 null 即无发起人信息的实例（setStartUserId(null) 不写，保持缺省空串） */
  private static ProcessInstanceRecord instance(
      final String startUserId, final String delegateUserId) {
    final ProcessInstanceRecord record = new ProcessInstanceRecord();
    record.setStartUserId(startUserId);
    record.setDelegateUserId(delegateUserId);
    return record;
  }

  @Test
  void startUserHimselfCancels() {
    cfg.setEnabled(true);
    operator("alice", List.of(), List.of());
    assertDoesNotThrow(() -> handler.checkCancelInstance(instance("alice", "")));
  }

  @Test
  void delegateInActForListCancels() {
    cfg.setEnabled(true);
    operator("carol", List.of("alice"), List.of());
    assertDoesNotThrow(() -> handler.checkCancelInstance(instance("alice", "")));
  }

  @Test
  void delegateOnInstanceCancels() {
    cfg.setEnabled(true);
    operator("carol", List.of(), List.of());
    assertDoesNotThrow(() -> handler.checkCancelInstance(instance("alice", "carol")));
  }

  @Test
  void outsiderCannotCancel() {
    cfg.setEnabled(true);
    operator("mallory", List.of("bob"), List.of());
    assertThrows(
        AuthorizationDeniedException.class, () -> handler.checkCancelInstance(instance("alice", "")));
  }

  @Test
  void adminCancelsAnyInstance() {
    cfg.setEnabled(true);
    cfg.setAdminGroups(List.of("admins"));
    operator("root", List.of(), List.of("admins"));
    assertDoesNotThrow(() -> handler.checkCancelInstance(instance("alice", "")));
  }

  @Test
  void instanceWithoutStartUserCannotBeProven() {
    // 无发起人信息的存量实例无法证明归属：名单里写着代办"空发起人"也不算
    cfg.setEnabled(true);
    operator("carol", List.of(""), List.of());
    assertThrows(
        AuthorizationDeniedException.class, () -> handler.checkCancelInstance(instance(null, "")));
  }

  @Test
  void disabledAuthorizationLetsAnyoneCancel() {
    cfg.setEnabled(false);
    when(writer.currentAuthorizationMetadata()).thenReturn(null);
    assertDoesNotThrow(() -> handler.checkCancelInstance(instance("alice", "")));
  }

  @Test
  void selfCancelClearsDelegate() {
    // 授权关闭同样判定：本人取消清空此前用命令指定的代办人
    operator("alice", List.of(), List.of());
    assertEquals("", handler.delegateOnCancel(instance("alice", "carol")));
  }

  @Test
  void actForCancelRecordsOperator() {
    operator("carol", List.of("alice"), List.of());
    assertEquals("carol", handler.delegateOnCancel(instance("alice", "")));
  }

  @Test
  void instanceDelegateCancelRecordsOperator() {
    operator("carol", List.of(), List.of());
    assertEquals("carol", handler.delegateOnCancel(instance("alice", "carol")));
  }

  @Test
  void adminCancelLeavesDelegateUntouched() {
    cfg.setEnabled(true);
    cfg.setAdminGroups(List.of("admins"));
    operator("root", List.of(), List.of("admins"));
    assertNull(handler.delegateOnCancel(instance("alice", "carol")));
  }

  @Test
  void withoutIdentityInstanceDelegateIsLeftToCommand() {
    when(writer.currentAuthorizationMetadata()).thenReturn("");
    assertNull(handler.delegateOnCancel(instance("alice", "carol")));
  }

  @Test
  void instanceWithoutStartUserHasNoDelegate() {
    operator("carol", List.of("alice"), List.of());
    assertNull(handler.delegateOnCancel(instance(null, "")));
  }

  @Test
  void startUserOrAdminMaySetInstanceDelegate() {
    cfg.setEnabled(true);
    cfg.setAdminGroups(List.of("admins"));
    operator("alice", List.of(), List.of());
    assertDoesNotThrow(() -> handler.checkSetDelegate(instance("alice", "")));
    operator("root", List.of(), List.of("admins"));
    assertDoesNotThrow(() -> handler.checkSetDelegate(instance("alice", "")));
  }

  @Test
  void othersMayNotSetInstanceDelegate() {
    // 名单里的代办人只代办办理，不能改实例的代办人
    cfg.setEnabled(true);
    operator("carol", List.of("alice"), List.of());
    assertThrows(
        AuthorizationDeniedException.class, () -> handler.checkSetDelegate(instance("alice", "")));
  }

  @Test
  void disabledAuthorizationLetsAnyoneSetInstanceDelegate() {
    cfg.setEnabled(false);
    assertDoesNotThrow(() -> handler.checkSetDelegate(instance("alice", "")));
  }

  @Test
  void onlyAdminMaySetStartUser() {
    // 发起人是审计身份：发起人本人也不能把自己的实例过户给别人
    cfg.setEnabled(true);
    cfg.setAdminGroups(List.of("admins"));
    operator("alice", List.of(), List.of());
    assertThrows(AuthorizationDeniedException.class, () -> handler.checkSetStartUser());
    operator("root", List.of(), List.of("admins"));
    assertDoesNotThrow(() -> handler.checkSetStartUser());
  }

  @Test
  void disabledAuthorizationLetsAnyoneSetStartUser() {
    cfg.setEnabled(false);
    assertDoesNotThrow(() -> handler.checkSetStartUser());
  }
}
