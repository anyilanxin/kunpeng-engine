/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container.addsign;

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnAddSignUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.ActivityInstanceBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BatchBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.BpmnActivityElementProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignPhase;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.ImmutableUserTaskRepository;

/**
 * 加签用户任务容器元素处理器：组活动实例（G）全生命周期收敛于此（对齐 {@code MultiInstanceElementProcessor} 位置感）。
 *
 * <p>激活完成按载荷建组员任务（并行转创建批/串行建首位）；COMPLETING(ref=0)=一个完成单位反馈（计数代数 + 串行推进）、
 * TERMINATING(ref=0)=一个终止单位反馈； 终止级联(ref&gt;0)先写终止事件，名下任务转终止批、级联子组转活动树终止批（双批 batchAfter 均指向 G
 * TERMINATED，行删幂等防重发）。
 *
 * <p>完成传播三规则：组员任务完结只反馈 G（结构保证）；顶级原任务完结被 E0 容器判定拦住（FROZEN 冻/AWAITING 扣出边）； E0 唯一放行源 = AFTER 组的
 * COMPLETED 反馈（BEFORE 组完结只解冻不传播，顶级展示态清除）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AddSignElementProcessor implements BpmnActivityElementProcessor<BpmnAddSignUserTask> {
  private final LogEventWriter writer;
  private final ActivityInstanceBehavior activityInstanceBehavior;
  private final AddSignBehavior addSignBehavior;
  private final BatchBehavior batchBehavior;
  private final VariableBehavior variableBehavior;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final ImmutableUserTaskRepository userTask;

  public AddSignElementProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    activityInstanceBehavior = behavior.activityInstanceBehavior();
    addSignBehavior = behavior.addSignBehavior();
    batchBehavior = behavior.batchBehavior();
    variableBehavior = behavior.variableBehavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    activityInstance = repository.instanceRepository();
    userTask = repository.userTaskRepository();
  }

  @Override
  public BpmnElementType getElementType() {
    return BpmnElementType.ADD_SIGN_USER_TASK;
  }

  @Override
  public Class<BpmnAddSignUserTask> getType() {
    return BpmnAddSignUserTask.class;
  }

  /** 1-1：激活（载荷/计数已由派生方写入 G 作用域） */
  @Override
  public void onActivating(
      final BpmnAddSignUserTask element, final ActivityContent activityContext) {
    final var value = activityContext.getValue();
    value.setStartTime(writer.millis());
    activityInstanceBehavior.onActivating(element, activityContext);
  }

  /** 1-3：激活完成——按载荷建组员任务（并行转创建批，批执行器逐轮创建；串行建首位，链式一次一个） */
  @Override
  public void onActivated(
      final BpmnAddSignUserTask element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setState(ActivityInstanceState.ACTIVE);
    value.setLifeCycle(ActivityInstanceLifeCycle.ACTIVATED);
    writer.addEvent(
        value.getActivityInstanceId(),
        ActivityInstanceLifeCycle.ACTIVATED,
        activityContext.getRequestId(),
        value);

    final AddSignBehavior.Payload payload = addSignBehavior.readPayload(activityContext);
    if (payload.mode == AddSignMode.PARALLEL) {
      // 并行展开转批处理：不在处理器内循环建任务，游标在 G 作用域、批可续跑
      batchBehavior.createTaskCreateBatch(activityContext);
      return;
    }
    final AddSignBehavior.Counters counters = addSignBehavior.readCounters(activityContext);
    addSignBehavior.createSignTaskAt(value, payload, counters, 0, activityContext.getRequestId());
  }

  /** 2-2：一个完成单位反馈（组员任务完结或级联子组完结）——计数代数 + 镜像刷新 + 完成判定 + 串行推进 */
  @Override
  public void onCompleting(
      final BpmnAddSignUserTask element, final ActivityContent activityContext) {
    final AddSignBehavior.Counters counters = addSignBehavior.readCounters(activityContext);
    counters.completed = counters.completed + 1;
    addSignBehavior.writeCounters(activityContext, counters);
    addSignBehavior.mirrorAnchorCount(
        activityContext.getValue(),
        addSignBehavior.readPayload(activityContext),
        counters,
        activityContext.getRequestId());
    if (counters.allEnded()) {
      activityInstanceBehavior.onCompleting(element, activityContext);
      return;
    }
    advanceSequential(element, activityContext, counters);
  }

  @Override
  public void onCompletingAfter(
      final BpmnAddSignUserTask element, final ActivityContent activityContext) {
    activityInstanceBehavior.onCompleting(element, activityContext);
  }

  /** 3-2：组完结——AFTER 向父级放行（顶级=E0 COMPLETING 走输出映射出边、级联=父组 COMPLETING 计数）；BEFORE 只解冻不传播 */
  @Override
  public void onCompleted(
      final BpmnAddSignUserTask element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    writer.addEvent(
        activityContext.getActivityInstanceId(),
        ActivityInstanceLifeCycle.COMPLETED,
        activityContext.getRequestId(),
        value);
    final AddSignBehavior.Payload payload = addSignBehavior.readPayload(activityContext);
    final ActivityInstanceRecord parent =
        activityInstance.getRecord(activityContext.getParentActivityInstanceId());
    if (payload.phase == AddSignPhase.AFTER) {
      if (parent != null) {
        // 唯一放行源：锚任务自身完成不传播活动，组完结才放行（顶级=E0、级联=父组计数）
        writer.addCommand(
            parent.getActivityInstanceId(),
            ActivityInstanceLifeCycle.COMPLETING,
            activityContext.getRequestId(),
            parent);
      }
    } else if (parent != null
        && parent.getActivityDefinitionType() != BpmnElementType.ADD_SIGN_USER_TASK) {
      // 前加签归零：顶级解冻展示态（锚任务行镜像已归零），等原任务最终完成
      parent.setAddSignState(AddSignState.NONE);
      writer.addEvent(
          parent.getActivityInstanceId(),
          ActivityInstanceLifeCycle.UPDATING,
          activityContext.getRequestId(),
          parent);
    }
    variableBehavior.variableHistory(activityContext);
  }

  /** 2-4：终止单位反馈（ref=0，单个组员终止/取消）——terminated 计数；全量结束直接转终止（不管完成条件）； 未尽串行顶位续链 */
  @Override
  public void onTerminating(
      final BpmnAddSignUserTask element, final ActivityContent activityContext) {
    final AddSignBehavior.Counters counters = addSignBehavior.readCounters(activityContext);
    if (activityContext.getBatchOperationReference() > 0) {
      // 终止级联：名下在途单位全部按终止计（对账一致），任务转终止批、级联子组转活动树终止批
      final int remaining =
          Math.max(0, counters.created - counters.completed - counters.terminated);
      counters.terminated = counters.terminated + remaining;
      addSignBehavior.writeCounters(activityContext, counters);
      activityInstanceBehavior.onTerminating(activityContext);
      final boolean hasTasks =
          !userTask.getAddSignTasks(activityContext.getActivityInstanceId()).isEmpty();
      final boolean hasChild =
          activityInstance.haveChildRecord(activityContext.getActivityInstanceId());
      if (hasTasks) {
        batchBehavior.createTaskTerminateBatch(activityContext);
      }
      if (hasChild) {
        batchBehavior.createActivityTerminatedBatch(
            activityContext, ActivityInstanceLifeCycle.TERMINATED);
      }
      if (!hasTasks && !hasChild) {
        activityInstanceBehavior.toTerminated(activityContext);
      }
      return;
    }
    counters.terminated = counters.terminated + 1;
    addSignBehavior.writeCounters(activityContext, counters);
    addSignBehavior.mirrorAnchorCount(
        activityContext.getValue(),
        addSignBehavior.readPayload(activityContext),
        counters,
        activityContext.getRequestId());
    if (counters.allEnded()) {
      // 全部结束：不管条件是否满足都转向终止
      activityInstanceBehavior.onTerminating(activityContext);
      activityInstanceBehavior.toTerminated(activityContext);
      return;
    }
    advanceSequential(element, activityContext, counters);
  }

  /** 3-4：组终止完成——批回报续外层终止链（或顶级尝试父级终止） */
  @Override
  public void onTerminated(
      final BpmnAddSignUserTask element, final ActivityContent activityContext) {
    final ActivityInstanceRecord value = activityContext.getValue();
    value.setEndTime(writer.millis());
    value.setState(ActivityInstanceState.TERMINATED);
    value.setLifeCycle(ActivityInstanceLifeCycle.TERMINATED);
    writer.addEvent(
        activityContext.getActivityInstanceId(),
        ActivityInstanceLifeCycle.TERMINATED,
        activityContext.getRequestId(),
        value);
    if (activityContext.getBatchOperationReference() > 0) {
      batchBehavior.handleTerminatedBatchReference(activityContext);
    } else {
      activityInstanceBehavior.tryParentTerminating(activityContext);
    }
    variableBehavior.variableHistory(activityContext);
  }

  /** 串行推进：名单未尽建下一位（游标在 G 作用域；追加并入的增量自然续上） */
  private void advanceSequential(
      final BpmnAddSignUserTask element,
      final ActivityContent activityContext,
      final AddSignBehavior.Counters counters) {
    final AddSignBehavior.Payload payload = addSignBehavior.readPayload(activityContext);
    if (payload.mode != AddSignMode.SEQUENTIAL || counters.cursor + 1 >= counters.total) {
      return;
    }
    addSignBehavior.createSignTaskAt(
        activityContext.getValue(),
        payload,
        counters,
        counters.cursor + 1,
        activityContext.getRequestId());
  }
}
