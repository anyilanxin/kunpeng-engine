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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignPhase;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.ImmutableUserTaskRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * 加签行为：加签任务的动态产生与主从引用（任务完成≠活动结束，结束判定归活动实例既有语义）。
 *
 * <p>首期约束：仅 ALL 规则、禁止级联；父级即原任务（活动实例 taskId 持有者），计数记在父任务行上。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AddSignBehavior {
  private final LogEventWriter writer;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final ImmutableUserTaskRepository userTask;

  public AddSignBehavior(final LogEventWriter writer) {
    this.writer = writer;
    activityInstance = writer.getRepository().instanceRepository();
    userTask = writer.getRepository().userTaskRepository();
  }

  /** 前加签触发：活动置 FROZEN（原任务禁止完成，终止不受限）。 */
  public void frozeActivity(final ActivityInstanceRecord instanceRecord, final long requestId) {
    instanceRecord.setAddSignState(AddSignState.FROZEN);
    writer.addEvent(
        instanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.UPDATING,
        requestId,
        instanceRecord);
  }

  /** 前加签归零：活动解冻（addSignState=NONE，assignee 保持不变），等原任务最终完成。 */
  public void unfrozeActivity(final ActivityInstanceRecord instanceRecord, final long requestId) {
    instanceRecord.setAddSignState(AddSignState.NONE);
    writer.addEvent(
        instanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.UPDATING,
        requestId,
        instanceRecord);
  }

  /**
   * 创建一个加签任务：挂原活动实例（同一容器），定义 key 指向解析期合成的虚拟节点， 走用户任务常规 CREATING→监听器→CREATED 链（虚拟节点无表达式，受理人直落）。
   *
   * @param originTask 原任务（承接定义 key/名称）
   * @param instanceRecord 原活动实例（承接流程/实例等公共字段）
   * @param assignee 本任务受理人
   * @param remainingAssignees 串行链剩余名单（并行传空列表）
   */
  public void createAddSignTask(
      final UserTaskRecord originTask,
      final ActivityInstanceRecord instanceRecord,
      final String assignee,
      final List<String> remainingAssignees,
      final AddSignPhase phase,
      final AddSignMode mode,
      final long requestId) {
    final UserTaskRecord addSignTask = new UserTaskRecord();
    addSignTask.wrap(instanceRecord);
    addSignTask
        .setTaskId(writer.nextCurrentSourceKey(instanceRecord.getProcessInstanceId()))
        .setTaskDefinitionKey(
            originTask.getTaskDefinitionKey() + BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX)
        .setTaskDefinitionName(originTask.getTaskDefinitionName())
        .setAssignee(assignee)
        .setLifeCycle(UserTaskLifeCycle.CREATING)
        .setAddSignRole(AddSignRole.ADD_SIGN)
        .setOriginActivityInstanceId(instanceRecord.getActivityInstanceId())
        .setAddSignPhase(phase)
        .setAddSignMode(mode)
        .setRemainingAssignees(remainingAssignees);
    writer.addCommand(addSignTask.getTaskId(), UserTaskLifeCycle.CREATING, requestId, addSignTask);
  }

  /**
   * 加签任务完成/终止后的父计数与放行判定（计数与状态变更同批级联，重放确定）。
   *
   * <p>串行链名单未耗尽 → 下一位顶上，父计数不动（恒 1）；名单已尽 → 父计数−1。 归零且前加签 → 解冻活动等原任务完成；归零且后加签 → 清 AWAITING 并直接结束当前活动
   * （onCompleting 输出映射此刻求值 → COMPLETED → 出边，走活动常规完成链）。 活动终止级联中：计数照减（对账一致），但不推进串行链、不解冻/放行（级联保证无残留）。
   *
   * @param addSignTask 刚结束的加签任务
   */
  public void onAddSignTaskEnded(final UserTaskRecord addSignTask, final long requestId) {
    final List<String> remaining = addSignTask.getRemainingAssignees();
    final ActivityInstanceRecord instanceRecord =
        activityInstance.getRecord(addSignTask.getOriginActivityInstanceId());
    final UserTaskRecord originTask = userTask.getRecord(instanceRecord.getTaskId());
    if (instanceRecord.getState() == ActivityInstanceState.TERMINATED) {
      originTask.setAddSignCount(originTask.getAddSignCount() - 1);
      writer.addEvent(originTask.getTaskId(), UserTaskLifeCycle.UPDATING, requestId, originTask);
      return;
    }
    if (addSignTask.getAddSignMode() == AddSignMode.SEQUENTIAL && !remaining.isEmpty()) {
      final String next = remaining.removeFirst();
      createAddSignTask(
          originTask,
          instanceRecord,
          next,
          remaining,
          addSignTask.getAddSignPhase(),
          addSignTask.getAddSignMode(),
          requestId);
      return;
    }
    originTask.setAddSignCount(originTask.getAddSignCount() - 1);
    writer.addEvent(originTask.getTaskId(), UserTaskLifeCycle.UPDATING, requestId, originTask);
    if (originTask.getAddSignCount() != 0) {
      return;
    }
    if (addSignTask.getAddSignPhase() == AddSignPhase.BEFORE) {
      unfrozeActivity(instanceRecord, requestId);
    } else {
      instanceRecord.setAddSignState(AddSignState.NONE);
      writer.addEvent(
          instanceRecord.getActivityInstanceId(),
          ActivityInstanceLifeCycle.UPDATING,
          requestId,
          instanceRecord);
      writer.addCommand(
          instanceRecord.getActivityInstanceId(),
          ActivityInstanceLifeCycle.COMPLETING,
          requestId,
          instanceRecord);
    }
  }

  /**
   * 后加签登记合并：完成前经独立命令或完成请求携带，统一落到任务行上的预登记名单（同模式追加去重、 跨模式拒绝），原任务完成时消费创建。
   *
   * @return 合并成功返回真（登记已随 UPDATING 事件落行）；跨模式冲突返回假（调用方回错误响应）
   */
  public boolean mergeAfterSignRegistration(
      final UserTaskRecord originTask,
      final List<String> incoming,
      final AddSignMode mode,
      final long requestId) {
    final List<String> registered = originTask.getAfterSignAssignees();
    if (registered.isEmpty()) {
      originTask.setAfterSignMode(mode);
      originTask.setAfterSignAssignees(incoming);
    } else if (originTask.getAfterSignMode() != mode) {
      return false;
    } else {
      final List<String> merged = new ArrayList<>(registered);
      incoming.stream().filter(assignee -> !merged.contains(assignee)).forEach(merged::add);
      originTask.setAfterSignAssignees(merged);
    }
    writer.addEvent(originTask.getTaskId(), UserTaskLifeCycle.UPDATING, requestId, originTask);
    return true;
  }

  /** 原任务完成且存在后加签预登记：消费登记创建加签任务、活动置 AWAITING 压下结束与出边 （任务完成≠活动结束）。父计数按并行全量/串行首位设置。 */
  public void onOriginCompletedWithAfterSigns(
      final UserTaskRecord originTask,
      final ActivityInstanceRecord instanceRecord,
      final long requestId) {
    final List<String> assignees = originTask.getAfterSignAssignees();
    final AddSignMode mode = originTask.getAfterSignMode();
    originTask.setAfterSignAssignees(List.of());
    originTask.setAddSignMode(mode);
    originTask.setAddSignCount(mode == AddSignMode.PARALLEL ? assignees.size() : 1);
    writer.addEvent(originTask.getTaskId(), UserTaskLifeCycle.UPDATING, requestId, originTask);

    instanceRecord.setAddSignState(AddSignState.AWAITING);
    writer.addEvent(
        instanceRecord.getActivityInstanceId(),
        ActivityInstanceLifeCycle.UPDATING,
        requestId,
        instanceRecord);

    if (mode == AddSignMode.PARALLEL) {
      for (final String assignee : assignees) {
        createAddSignTask(
            originTask, instanceRecord, assignee, List.of(), AddSignPhase.AFTER, mode, requestId);
      }
    } else {
      createAddSignTask(
          originTask,
          instanceRecord,
          assignees.getFirst(),
          assignees.subList(1, assignees.size()),
          AddSignPhase.AFTER,
          mode,
          requestId);
    }
  }
}
