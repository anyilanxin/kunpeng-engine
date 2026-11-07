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
package com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl;

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.protocol.business.EngineReservedVariables;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignPhase;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.ImmutableUserTaskRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 加签行为（二期执行实例模型）：加签组活动实例（G）的派生、载荷/计数簿记与组员任务创建。
 *
 * <p>每个加签组派生一个子活动实例挂加签容器元素（parent=锚任务活动实例，taskId=锚任务 id）；组员任务与业务变量全部挂 G 作用域； 组完成判定（计数代数/完成条件）归
 * {@code AddSignElementProcessor}，本行为只提供派生与读写原语。
 *
 * <p>追加加签并入既有组（同相位同模式）：名单去重追加、total 随增，创建游标以 G 作用域为权威（批耗尽后门户可再挂批续跑）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AddSignBehavior {
  /** 组展开载荷（G 作用域变量；契约单源 {@link EngineReservedVariables}） */
  public static final String ADD_SIGN_PAYLOAD = EngineReservedVariables.ADD_SIGN_PAYLOAD;

  /** 组计数器（G 作用域变量；契约单源 {@link EngineReservedVariables}） */
  public static final String ADD_SIGN_COUNTERS = EngineReservedVariables.ADD_SIGN_COUNTERS;

  private static final String PAYLOAD_ASSIGNEES = "assignees";
  private static final String PAYLOAD_MODE = "mode";
  private static final String PAYLOAD_PHASE = "phase";
  private static final String PAYLOAD_TASK_KEY = "taskKey";
  private static final String PAYLOAD_TASK_NAME = "taskName";
  private static final String PAYLOAD_DEPTH = "depth";
  private static final String COUNTERS_TOTAL = "total";
  private static final String COUNTERS_CREATED = "created";
  private static final String COUNTERS_COMPLETED = "completed";
  private static final String COUNTERS_TERMINATED = "terminated";
  private static final String COUNTERS_CURSOR = "cursor";

  private final LogEventWriter writer;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final ImmutableUserTaskRepository userTask;
  private final ImmutableProcessInstanceRepository processInstance;
  private final VariableBehavior variableBehavior;

  public AddSignBehavior(final LogEventWriter writer, final VariableBehavior variableBehavior) {
    this.writer = writer;
    this.variableBehavior = variableBehavior;
    final ImmutableBusinessRepository repository = writer.getRepository();
    activityInstance = repository.instanceRepository();
    userTask = repository.userTaskRepository();
    processInstance = repository.processInstanceRepository();
  }

  private BatchBehavior batchBehavior() {
    return writer.behavior().batchBehavior();
  }

  /** 锚任务当前在途组实例（同锚任务至多一个在途组；无则 null） */
  public ActivityInstanceRecord findInFlightGroup(final UserTaskRecord anchorTask) {
    final List<Long> children =
        activityInstance.getChildRecord(
            anchorTask.getActivityInstanceId(),
            2,
            record ->
                record.getActivityDefinitionType() == BpmnElementType.ADD_SIGN_USER_TASK
                    && record.getTaskId() == anchorTask.getTaskId()
                    && inFlight(record));
    for (final long childId : children) {
      final ActivityInstanceRecord group = activityInstance.getRecord(childId);
      if (group != null) {
        return group;
      }
    }
    return null;
  }

  /** 活动名下是否存在在途加签组（任意锚） */
  public boolean hasInFlightChildGroup(final long activityInstanceId) {
    return !activityInstance
        .getChildRecord(
            activityInstanceId,
            1,
            record ->
                record.getActivityDefinitionType() == BpmnElementType.ADD_SIGN_USER_TASK
                    && inFlight(record))
        .isEmpty();
  }

  /**
   * 派生加签组活动实例 G：copy 锚活动实例形态（MI createChild 同款），parent/feedback=锚活动， taskId=锚任务 id（组锚点）， 载荷与计数器落 G
   * 作用域后发 ACTIVATING（组生命周期由此归容器处理器）。
   *
   * <p>顶级（锚活动非容器）时同步写 E0 加签展示态（BEFORE=FROZEN/AWAITING），并落锚任务行镜像计数。
   */
  public void spawnGroup(
      final UserTaskRecord anchorTask,
      final ActivityInstanceRecord anchorActivity,
      final List<String> assignees,
      final AddSignPhase phase,
      final AddSignMode mode,
      final long requestId) {
    final ActivityInstanceRecord group = anchorActivity.copy();
    group
        .setActivityInstanceId(writer.nextCurrentSourceKey(anchorActivity.getProcessInstanceId()))
        .setParentActivityInstanceId(anchorActivity.getActivityInstanceId())
        .setFeedBackParentActivityInstanceId(anchorActivity.getActivityInstanceId())
        .setStartActivityInstanceId(anchorActivity.getActivityInstanceId())
        .setStartActivityDefinitionKey(anchorActivity.getActivityDefinitionKey())
        .setSequenceCounter(
            processInstance.getSequenceCounter(anchorActivity.getProcessInstanceId()))
        .setActivityDefinitionKey(virtualKeyOf(anchorTask.getTaskDefinitionKey()))
        .setActivityDefinitionType(BpmnElementType.ADD_SIGN_USER_TASK)
        .setActivityDefinitionName(anchorTask.getTaskDefinitionName())
        .setTaskId(anchorTask.getTaskId())
        .setAddSignState(AddSignState.NONE);

    final ActivityContent groupContent = new ActivityContent(group, requestId);
    variableBehavior.variableUpdateInternal(
        groupContent,
        Map.of(
            ADD_SIGN_PAYLOAD,
            Map.of(
                PAYLOAD_ASSIGNEES,
                assignees,
                PAYLOAD_MODE,
                mode.name(),
                PAYLOAD_PHASE,
                phase.name(),
                PAYLOAD_TASK_KEY,
                rawKeyOf(anchorTask.getTaskDefinitionKey()),
                PAYLOAD_TASK_NAME,
                anchorTask.getTaskDefinitionName(),
                PAYLOAD_DEPTH,
                anchorTask.getAddSignDepth() + 1)));
    variableBehavior.variableUpdateInternal(
        groupContent,
        Map.of(
            ADD_SIGN_COUNTERS,
            Map.of(
                COUNTERS_TOTAL,
                assignees.size(),
                COUNTERS_CREATED,
                0,
                COUNTERS_COMPLETED,
                0,
                COUNTERS_TERMINATED,
                0,
                COUNTERS_CURSOR,
                -1)));

    // 锚任务行镜像（导出面快照：在途数）+ 顶级展示态
    final UserTaskRecord anchor = userTask.getRecord(anchorTask.getTaskId());
    if (anchor != null) {
      anchor.setAddSignMode(mode);
      anchor.setAddSignCount(mode == AddSignMode.PARALLEL ? assignees.size() : 1);
      writer.addEvent(anchor.getTaskId(), UserTaskLifeCycle.UPDATING, requestId, anchor);
    }
    if (anchorActivity.getActivityDefinitionType() != BpmnElementType.ADD_SIGN_USER_TASK) {
      anchorActivity.setAddSignState(
          phase == AddSignPhase.BEFORE ? AddSignState.FROZEN : AddSignState.AWAITING);
      writer.addEvent(
          anchorActivity.getActivityInstanceId(),
          ActivityInstanceLifeCycle.UPDATING,
          requestId,
          anchorActivity);
    }
    writer.addCommand(
        group.getActivityInstanceId(), ActivityInstanceLifeCycle.ACTIVATING, requestId, group);
  }

  /**
   * 追加加签并入既有组：名单去重追加、total 随增、锚行镜像同步加；并行且旧批已耗尽（游标到旧表尾）时再挂一轮创建批续跑增量。
   *
   * @return 模式冲突返回假（调用方回错误响应）；名单全重复时无操作返回真
   */
  public boolean mergeIntoGroup(
      final ActivityInstanceRecord group,
      final List<String> incoming,
      final AddSignMode mode,
      final long requestId) {
    final ActivityContent groupContent = new ActivityContent(group, requestId);
    final Payload payload = readPayload(groupContent);
    final Counters counters = readCounters(groupContent);
    if (payload.mode != mode) {
      return false;
    }
    final List<String> merged = new ArrayList<>(payload.assignees);
    incoming.stream().filter(assignee -> !merged.contains(assignee)).forEach(merged::add);
    if (merged.size() == payload.assignees.size()) {
      return true;
    }
    final int oldTotal = counters.total;
    payload.assignees = merged;
    counters.total = merged.size();
    writePayload(groupContent, payload);
    writeCounters(groupContent, counters);
    mirrorAnchorCount(group, payload, counters, requestId);
    if (payload.mode == AddSignMode.PARALLEL
        && counters.cursor + 1 >= oldTotal
        && counters.cursor < counters.total) {
      // 旧批已耗尽（游标到旧表尾）而名单有增量：再挂一轮批从游标续跑（批在途时后续轮次直接读到追加名单，不重复挂）
      batchBehavior().createTaskCreateBatch(groupContent);
    }
    return true;
  }

  /**
   * 批执行器逐轮调用：按 G 载荷/游标创建下一个组员任务并推进游标。
   *
   * @return 是否还有剩余待创建（false 时批收尾）
   */
  public boolean createNextSignTask(final ActivityInstanceRecord group, final long requestId) {
    final ActivityContent groupContent = new ActivityContent(group, requestId);
    final Payload payload = readPayload(groupContent);
    final Counters counters = readCounters(groupContent);
    if (counters.cursor + 1 >= counters.total) {
      return false;
    }
    createSignTaskAt(group, payload, counters, counters.cursor + 1, requestId);
    return counters.cursor + 1 < counters.total;
  }

  /** 创建指定下标的组员任务（挂 G；串行链名单为导出面快照），并推进游标/创建数 */
  public void createSignTaskAt(
      final ActivityInstanceRecord group,
      final Payload payload,
      final Counters counters,
      final int index,
      final long requestId) {
    // 仓储 getRecord 返回共享缓冲：rootActivityInstanceId 沿父链再查会覆写 group 引用本身，
    // 之后 writeCounters 会把计数器错写到父作用域（组游标永滞初值=并行展开失控根因）。
    // 先 copy 固化组自身（spawnGroup 同款防线），后续读写一律以快照为准。
    final ActivityInstanceRecord groupSnapshot = group.copy();
    // 加签许可随创建固化（与门户 cascadeRejectReason 同口径）：组员可再加签 = 原元素开启 cascade 且本任务深度+1 ≤ maxDepth
    final BpmnUserTask originElement =
        writer
            .getRepository()
            .bpmnResourceRepository()
            .getRuntime(groupSnapshot.getProcessDefinitionId())
            .executableProcess()
            .getElementById(payload.taskKey, BpmnUserTask.class);
    final boolean cascadeAllowed =
        originElement != null
            && originElement.isAddSignCascade()
            && payload.depth + 1 <= originElement.getAddSignMaxDepth();
    final UserTaskRecord signTask = new UserTaskRecord();
    signTask.wrap(groupSnapshot);
    signTask
        .setTaskId(writer.nextCurrentSourceKey(groupSnapshot.getProcessInstanceId()))
        .setTaskDefinitionKey(groupSnapshot.getActivityDefinitionKey())
        .setTaskDefinitionName(payload.taskName)
        .setAssignee(payload.assignees.get(index))
        .setLifeCycle(UserTaskLifeCycle.CREATING)
        .setAddSignRole(AddSignRole.ADD_SIGN)
        .setParentTaskId(groupSnapshot.getTaskId())
        .setRootActivityInstanceId(rootActivityInstanceId(groupSnapshot))
        .setAddSignPhase(payload.phase)
        .setAddSignMode(payload.mode)
        .setRemainingAssignees(
            payload.mode == AddSignMode.SEQUENTIAL
                ? List.copyOf(payload.assignees.subList(index + 1, payload.assignees.size()))
                : List.of())
        .setAddSignDepth(payload.depth)
        .setAddSignAllowed(cascadeAllowed);
    counters.cursor = index;
    counters.created = index + 1;
    final ActivityContent groupContent = new ActivityContent(groupSnapshot, requestId);
    writeCounters(groupContent, counters);
    writer.addCommand(signTask.getTaskId(), UserTaskLifeCycle.CREATING, requestId, signTask);
  }

  /**
   * 组员任务完结/终止/取消后的簿记：锚行镜像刷新 + 组计数反馈（G COMPLETING=完成单位、TERMINATING=终止单位， ref=0 走容器计数路径）。
   *
   * <p>组已终态（级联窗口）只刷镜像不反馈；锚行已删（先行终止）无载体跳过。
   *
   * @param signTask 刚结束的组员任务
   * @param terminated true=终止/取消路径
   */
  public void onSignTaskEnded(
      final UserTaskRecord signTask, final long requestId, final boolean terminated) {
    final ActivityInstanceRecord group =
        activityInstance.getRecord(signTask.getActivityInstanceId());
    if (group == null) {
      // 组行已随终止批收尾删除：无簿记可做
      return;
    }
    if (!inFlight(group)) {
      // 组终止/完结级联中：计数由容器 onTerminating 一次性对账，不逐任务反馈
      return;
    }
    writer.addCommand(
        group.getActivityInstanceId(),
        terminated ? ActivityInstanceLifeCycle.TERMINATING : ActivityInstanceLifeCycle.COMPLETING,
        requestId,
        group);
  }

  /**
   * 任务完结且存在后加签预登记（原任务或组员任务统一）：消费登记派生 AFTER 组 G（锚=本任务、parent=其活动实例）， 镜像落行；顶级时活动置 AWAITING
   * 扣出边。锚任务完成不向本组反馈计数——延迟传播，G 完结时才作为完成单位计入。
   */
  public void onTaskCompletedWithAfterSigns(
      final UserTaskRecord task,
      final ActivityInstanceRecord activityRecord,
      final long requestId) {
    final List<String> assignees = task.getAfterSignAssignees();
    final AddSignMode mode = task.getAfterSignMode();
    task.setAfterSignAssignees(List.of());
    writer.addEvent(task.getTaskId(), UserTaskLifeCycle.UPDATING, requestId, task);
    spawnGroup(task, activityRecord, assignees, AddSignPhase.AFTER, mode, requestId);
  }

  /**
   * 后加签登记合并：完成前经独立命令或完成请求携带，统一落到任务行上的预登记名单（同模式追加去重、跨模式拒绝）， 完结时消费派生。
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

  /** 组载荷读取（G 作用域） */
  public Payload readPayload(final ActivityContent groupContent) {
    final Map<String, Object> direct = variableBehavior.getDirectVariable(groupContent);
    final Payload payload = new Payload();
    if (direct == null || direct.isEmpty()) {
      return payload;
    }
    @SuppressWarnings("unchecked")
    final Map<String, Object> raw = (Map<String, Object>) direct.get(ADD_SIGN_PAYLOAD);
    if (raw == null) {
      return payload;
    }
    @SuppressWarnings("unchecked")
    final List<String> assignees = (List<String>) raw.get(PAYLOAD_ASSIGNEES);
    payload.assignees = assignees == null ? List.of() : assignees;
    payload.mode = AddSignMode.valueOf((String) raw.get(PAYLOAD_MODE));
    payload.phase = AddSignPhase.valueOf((String) raw.get(PAYLOAD_PHASE));
    payload.taskKey = (String) raw.get(PAYLOAD_TASK_KEY);
    payload.taskName = (String) raw.get(PAYLOAD_TASK_NAME);
    payload.depth = ((Number) raw.getOrDefault(PAYLOAD_DEPTH, 1)).intValue();
    return payload;
  }

  /** 组计数器读取（G 作用域；未写入时取初值） */
  public Counters readCounters(final ActivityContent groupContent) {
    final Map<String, Object> direct = variableBehavior.getDirectVariable(groupContent);
    final Counters counters = new Counters(0, 0, 0, 0, -1);
    if (direct == null || direct.isEmpty()) {
      return counters;
    }
    @SuppressWarnings("unchecked")
    final Map<String, Object> raw = (Map<String, Object>) direct.get(ADD_SIGN_COUNTERS);
    if (raw == null) {
      return counters;
    }
    // DocumentCodec 契约：文档数值读侧一律回读为 Long（写侧 Number 统一 longValue），整型必须经 Number 取值
    counters.total = ((Number) raw.getOrDefault(COUNTERS_TOTAL, 0)).intValue();
    counters.created = ((Number) raw.getOrDefault(COUNTERS_CREATED, 0)).intValue();
    counters.completed = ((Number) raw.getOrDefault(COUNTERS_COMPLETED, 0)).intValue();
    counters.terminated = ((Number) raw.getOrDefault(COUNTERS_TERMINATED, 0)).intValue();
    counters.cursor = ((Number) raw.getOrDefault(COUNTERS_CURSOR, -1)).intValue();
    return counters;
  }

  /** 组计数器写回（G 作用域） */
  public void writeCounters(final ActivityContent groupContent, final Counters counters) {
    final Map<String, Object> countersMap = new HashMap<>();
    countersMap.put(COUNTERS_TOTAL, counters.total);
    countersMap.put(COUNTERS_CREATED, counters.created);
    countersMap.put(COUNTERS_COMPLETED, counters.completed);
    countersMap.put(COUNTERS_TERMINATED, counters.terminated);
    countersMap.put(COUNTERS_CURSOR, counters.cursor);
    variableBehavior.variableUpdateInternal(groupContent, Map.of(ADD_SIGN_COUNTERS, countersMap));
  }

  /** 组载荷写回（G 作用域，追加合并用） */
  private void writePayload(final ActivityContent groupContent, final Payload payload) {
    variableBehavior.variableUpdateInternal(
        groupContent,
        Map.of(
            ADD_SIGN_PAYLOAD,
            Map.of(
                PAYLOAD_ASSIGNEES,
                payload.assignees,
                PAYLOAD_MODE,
                payload.mode.name(),
                PAYLOAD_PHASE,
                payload.phase.name(),
                PAYLOAD_TASK_KEY,
                payload.taskKey,
                PAYLOAD_TASK_NAME,
                payload.taskName,
                PAYLOAD_DEPTH,
                payload.depth)));
  }

  /** 锚任务行镜像刷新（在途数快照：并行=total-已结束、串行=未全完为 1/全完为 0；行不在跳过）。计数递增后调用（容器处理器） */
  public void mirrorAnchorCount(
      final ActivityInstanceRecord group,
      final Payload payload,
      final Counters counters,
      final long requestId) {
    final UserTaskRecord anchor = userTask.getRecord(group.getTaskId());
    if (anchor == null) {
      return;
    }
    final int ended = counters.completed + counters.terminated;
    final int mirror =
        payload.mode == AddSignMode.PARALLEL
            ? Math.max(0, counters.total - ended)
            : (ended >= counters.total ? 0 : 1);
    anchor.setAddSignMode(payload.mode);
    anchor.setAddSignCount(mirror);
    writer.addEvent(anchor.getTaskId(), UserTaskLifeCycle.UPDATING, requestId, anchor);
  }

  /** G 的根活动实例 id=E0（沿 parent 链上溯到首个非容器实例；组已拆则 0），全层级加签任务共用同一根 */
  public long rootActivityInstanceId(final ActivityInstanceRecord group) {
    ActivityInstanceRecord current = group;
    while (current != null
        && current.getActivityDefinitionType() == BpmnElementType.ADD_SIGN_USER_TASK) {
      current = activityInstance.getRecord(current.getParentActivityInstanceId());
    }
    return current == null ? 0 : current.getActivityInstanceId();
  }

  /**
   * 加签门户的级联门控（统一口径）：顶级任务（深度 0）只看模型许可声明；级联（给加签任务再加签）需建模开启 cascade，且给深度 D 的锚加签 = 产生第 D+1 级任务，须 D+1 ≤
   * maxDepth（级联层数上限，仅 cascade 开启时有意义；未开启时有效为 1——只能一级加签，深度 0 锚独占）。
   *
   * <p>深度直读任务行 ADD_SIGN_DEPTH（创建时随组载荷写入），不沿 parentTaskId 链现场推导。
   *
   * @return null=放行；否则=拒绝原因（直接作错误响应消息）
   */
  public String cascadeRejectReason(final UserTaskRecord anchorTask, final BpmnUserTask element) {
    final int depth = anchorTask.getAddSignDepth();
    if (depth == 0) {
      return null;
    }
    if (!element.isAddSignCascade()) {
      return "建模未允许级联加签（kunpeng:addSign 未开启 cascade，加签任务不可再加签）";
    }
    if (depth + 1 > element.getAddSignMaxDepth()) {
      return "超出建模允许的加签级联层数（当前第 "
          + (depth + 1)
          + " 级，maxDepth="
          + element.getAddSignMaxDepth()
          + "）";
    }
    return null;
  }

  private static boolean inFlight(final ActivityInstanceRecord record) {
    return record.getLifeCycle() == ActivityInstanceLifeCycle.ACTIVATING
        || record.getLifeCycle() == ActivityInstanceLifeCycle.ACTIVATED;
  }

  private static String virtualKeyOf(final String taskDefinitionKey) {
    return taskDefinitionKey.endsWith(BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX)
        ? taskDefinitionKey
        : taskDefinitionKey + BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX;
  }

  private static String rawKeyOf(final String taskDefinitionKey) {
    return taskDefinitionKey.endsWith(BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX)
        ? taskDefinitionKey.substring(
            0, taskDefinitionKey.length() - BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX.length())
        : taskDefinitionKey;
  }

  /** 组展开载荷（G 作用域） */
  public static class Payload {
    public List<String> assignees = List.of();
    public AddSignMode mode;
    public AddSignPhase phase;
    public String taskKey;
    public String taskName;

    /** 组员任务的加签层级（=锚任务层级+1，spawnGroup 时随载荷定死） */
    public int depth = 1;
  }

  /** 组计数器（G 作用域）：total=组员总数（追加随增）、created=已建数、cursor=最后建成的下标、completed/terminated=已结束单位数 */
  public static class Counters {
    public int total;
    public int created;
    public int completed;
    public int terminated;
    public int cursor;

    public Counters(
        final int total,
        final int created,
        final int completed,
        final int terminated,
        final int cursor) {
      this.total = total;
      this.created = created;
      this.completed = completed;
      this.terminated = terminated;
      this.cursor = cursor;
    }

    /** 完成判定（计数代数，不扫子行）：已结束单位覆盖全量名单 */
    public boolean allEnded() {
      return completed + terminated >= total;
    }
  }
}
