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

import com.anyilanxin.kunpeng.configuration.broker.AuthorizationCfg;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import java.util.List;
import org.springframework.beans.factory.BeanFactory;

/**
 * 引擎侧授权校验：从信封授权元数据解析操作人，按命令规则校验（重放一致，判据全部来自记录自身）。未启用（默认关）/无配置时全部放行；admin 白名单（用户/组）旁路。
 *
 * <p>代为办理：操作人的代办名单（{@link Authorization#actFor()}）含属主（任务审批人 /
 * 流程发起人）、或属主身上的代办人就是操作人时，视同代办人办理，可完成任务、取消流程实例。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class AuthorizationHandler {
  private final LogEventWriter writer;

  public AuthorizationHandler(final LogEventWriter writer) {
    this.writer = writer;
  }

  /** 取消流程实例：发起人匹配 → 代办人办理（实例上的代办人或代办名单含发起人）；实例无发起人信息（存量旧数据）无法证明归属，拒绝。 */
  public void checkCancelInstance(final ProcessInstanceRecord instanceRecord) {
    final AuthorizationCfg cfg = cfg();
    if (!enabled(cfg)) {
      return;
    }
    final Authorization operator = currentOperator();
    if (isAdmin(cfg, operator)) {
      return;
    }
    final String startUserId = instanceRecord.getStartUserId();
    if (startUserId == null || startUserId.isEmpty()) {
      throw new AuthorizationDeniedException("流程实例无发起人信息，无法校验取消权限");
    }
    if (!startUserId.equals(operator.userId())
        && !actsAsDelegate(startUserId, instanceRecord.getDelegateUserId(), operator)) {
      throw new AuthorizationDeniedException(
          "操作人(" + operator.userId() + ")非流程发起人(" + startUserId + ")或其代办人，无权取消流程实例");
    }
  }

  /** 签收用户任务：返回操作人身份供设置签收人。签收必须携带操作人身份（签收人来自身份而非请求参数）；候选人归属校验跟随 enabled 开关。 */
  public Authorization requireClaimOperator(final UserTaskRecord userTask) {
    final Authorization operator = currentOperator();
    if (operator.userId().isEmpty()) {
      throw new AuthorizationDeniedException("签收需要携带操作人身份（授权元数据缺失 userId）");
    }
    final AuthorizationCfg cfg = cfg();
    if (!enabled(cfg) || isAdmin(cfg, operator)) {
      return operator;
    }
    final List<String> candidateUsers = userTask.getCandidateUsers();
    if (candidateUsers != null && !candidateUsers.isEmpty()) {
      if (!candidateUsers.contains(operator.userId())) {
        throw new AuthorizationDeniedException("操作人(" + operator.userId() + ")非任务候选审批人，无权签收");
      }
      return operator;
    }
    final List<String> candidateGroups = userTask.getCandidateGroups();
    if (candidateGroups != null && !candidateGroups.isEmpty()) {
      final boolean matched = operator.groups().stream().anyMatch(candidateGroups::contains);
      if (!matched) {
        throw new AuthorizationDeniedException("操作人所属组不在任务候选审批组内，无权签收");
      }
    }
    return operator;
  }

  /** 完成用户任务：审批人匹配 → 代办人办理（任务上的代办人或代办名单含审批人）→ 候选审批人包含操作人 → 候选审批组与操作人所属组相交；三者全空（公开任务）放行。 */
  public void checkCompleteTask(final UserTaskRecord userTask) {
    final AuthorizationCfg cfg = cfg();
    if (!enabled(cfg)) {
      return;
    }
    final Authorization operator = currentOperator();
    if (isAdmin(cfg, operator)) {
      return;
    }
    final String assignee = userTask.getAssignee();
    if (assignee != null && !assignee.isEmpty()) {
      if (!assignee.equals(operator.userId())
          && !actsAsDelegate(assignee, userTask.getDelegateUserId(), operator)) {
        throw new AuthorizationDeniedException(
            "操作人(" + operator.userId() + ")非任务审批人(" + assignee + ")或其代办人，无权完成任务");
      }
      return;
    }
    final List<String> candidateUsers = userTask.getCandidateUsers();
    if (candidateUsers != null && !candidateUsers.isEmpty()) {
      if (!candidateUsers.contains(operator.userId())) {
        throw new AuthorizationDeniedException("操作人(" + operator.userId() + ")非任务候选审批人，无权完成任务");
      }
      return;
    }
    final List<String> candidateGroups = userTask.getCandidateGroups();
    if (candidateGroups != null && !candidateGroups.isEmpty()) {
      final boolean groupMatched = operator.groups().stream().anyMatch(candidateGroups::contains);
      if (!groupMatched) {
        throw new AuthorizationDeniedException(
            "操作人(" + operator.userId() + ")所属组不在任务候选审批组内，无权完成任务");
      }
    }
  }

  /** 取消签收用户任务：操作人必须是当前签收人（签收人匹配跟随授权开关，与完成任务同形态）；admin 白名单旁路。 */
  public void checkUnclaim(final UserTaskRecord userTask) {
    final AuthorizationCfg cfg = cfg();
    if (!enabled(cfg)) {
      return;
    }
    final Authorization operator = currentOperator();
    if (isAdmin(cfg, operator)) {
      return;
    }
    final String assignee = userTask.getAssignee();
    if (!operator.userId().equals(assignee)) {
      throw new AuthorizationDeniedException(
          "操作人(" + operator.userId() + ")非任务签收人(" + assignee + ")，无权取消签收");
    }
  }

  /** 改派用户任务：管理员动作——启用时操作人须命中 admin 白名单（用户/组），未启用放行（渐进语义同取消/完成）。 */
  public void checkReassign() {
    requireAdmin("改派任务");
  }

  /** 修改流程发起人：管理员动作（发起人是审计身份，改它等于过户），规则同改派。 */
  public void checkSetStartUser() {
    requireAdmin("修改流程发起人");
  }

  /**
   * 设置代办人：审批人本人（给自己找代办）或管理员的动作；未启用放行，与改派同形态。
   *
   * @param userTask 须已指定审批人的任务
   */
  public void checkSetDelegate(final UserTaskRecord userTask) {
    requireOwnerOrAdmin(userTask.getAssignee(), "任务审批人");
  }

  /**
   * 设置流程实例代办人：发起人本人或管理员的动作；未启用放行。
   *
   * @param instanceRecord 须已有发起人的流程实例
   */
  public void checkSetDelegate(final ProcessInstanceRecord instanceRecord) {
    requireOwnerOrAdmin(instanceRecord.getStartUserId(), "流程发起人");
  }

  /**
   * 完成任务时的代办人 id（判定不跟随授权开关，未接入授权体系也能记录）：审批人本人完成返回空串（自办，清空此前指定的代办人）；操作人以代办人身份完成返回操作人
   * id；其余情况（未指定审批人的候选任务、不带身份、管理员旁路）返回 null，表示任务上的代办人保持不动。
   */
  public String delegateOnComplete(final UserTaskRecord userTask) {
    return delegateOnTerminal(userTask.getAssignee(), userTask.getDelegateUserId());
  }

  /**
   * 取消流程实例时的代办人 id，规则同完成任务（判定不跟随授权开关）：发起人本人取消返回空串；操作人以代办人身份取消返回操作人 id；其余情况（实例无发起人、不带身份、管理员旁路）返回
   * null，表示实例上的代办人保持不动。
   */
  public String delegateOnCancel(final ProcessInstanceRecord instanceRecord) {
    return delegateOnTerminal(instanceRecord.getStartUserId(), instanceRecord.getDelegateUserId());
  }

  /** 属主经手返回空串（清空代办人）、代办身份经手返回操作人 id、其余返回 null（代办人不动） */
  private String delegateOnTerminal(final String ownerId, final String delegateUserId) {
    if (ownerId == null || ownerId.isEmpty()) {
      return null;
    }
    final Authorization operator = currentOperator();
    final String userId = operator.userId();
    if (userId.isEmpty()) {
      return null;
    }
    if (ownerId.equals(userId)) {
      return "";
    }
    return actsAsDelegate(ownerId, delegateUserId, operator) ? userId : null;
  }

  /** 操作人是否以代办人身份办理：属主（任务审批人 / 流程发起人）身上的代办人就是操作人，或操作人的代办名单含属主 */
  private boolean actsAsDelegate(
      final String ownerId, final String delegateUserId, final Authorization operator) {
    return (!delegateUserId.isEmpty() && delegateUserId.equals(operator.userId()))
        || operator.actFor().contains(ownerId);
  }

  /** 启用时操作人须命中 admin 白名单，未启用放行。 */
  private void requireAdmin(final String action) {
    final AuthorizationCfg cfg = cfg();
    if (!enabled(cfg)) {
      return;
    }
    final Authorization operator = currentOperator();
    if (isAdmin(cfg, operator)) {
      return;
    }
    throw new AuthorizationDeniedException(
        "操作人(" + operator.userId() + ")非管理员（admin 白名单），无权" + action);
  }

  /** 启用时操作人须是属主本人或管理员，未启用放行。 */
  private void requireOwnerOrAdmin(final String ownerId, final String ownerRole) {
    final AuthorizationCfg cfg = cfg();
    if (!enabled(cfg)) {
      return;
    }
    final Authorization operator = currentOperator();
    if (isAdmin(cfg, operator) || ownerId.equals(operator.userId())) {
      return;
    }
    throw new AuthorizationDeniedException(
        "操作人(" + operator.userId() + ")非" + ownerRole + "(" + ownerId + ")或管理员，无权设置代办人");
  }

  private Authorization currentOperator() {
    return Authorization.fromJson(writer.currentAuthorizationMetadata());
  }

  private boolean enabled(final AuthorizationCfg cfg) {
    return cfg != null && cfg.isEnabled();
  }

  private boolean isAdmin(final AuthorizationCfg cfg, final Authorization operator) {
    return operator.groups().stream().anyMatch(cfg.getAdminGroups()::contains);
  }

  /**
   * 从 Spring 容器解析 BrokerCfg 取内嵌授权配置（dist broker/restore profile 注册）；容器无该 bean（裸单测）视为未配置=功能关闭（默认关语义）
   */
  private AuthorizationCfg cfg() {
    final BeanFactory beanFactory = writer.getBeanFactory();
    if (beanFactory == null) {
      return null;
    }
    final BrokerCfg brokerCfg = beanFactory.getBeanProvider(BrokerCfg.class).getIfAvailable();
    return brokerCfg == null ? null : brokerCfg.getAuthorization();
  }
}
