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
package com.anyilanxin.kunpeng.engine.bpmn.command.batch.processor;

import com.anyilanxin.kunpeng.bpm.common.BpmnElementType;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.batch.AbstractBatchProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BatchBehavior;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.ImmutableUserTaskRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 加签整组终止批执行器：活动终止时名下在途任务（原任务+加签任务，行在即在途——终态任务删行天然过滤） 逐个终止，游标=已处理
 * taskId（自排命令在首个任务终止级联之前入队，下轮查询需跳过已发者）。
 *
 * <p>最后一个在途任务终止命令发出后即收尾：batchAfter 回活动 TERMINATED 续终止链（外层引用透传， 流程取消等外层批继续闭环），活动行在其 TERMINATED
 * 事件前始终存在。后加签 AWAITING 下原任务已 COMPLETED 删行，收集时自然跳过（不会被误终止）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BatchTaskTerminateExecutorProcessor extends AbstractBatchProcessor {
  private final ImmutableUserTaskRepository userTask;
  private final BatchBehavior batchBehavior;

  public BatchTaskTerminateExecutorProcessor(final LogEventWriter writer) {
    super(writer);
    final ImmutableBusinessRepository repository = writer.getRepository();
    userTask = repository.userTaskRepository();
    batchBehavior = writer.behavior().batchBehavior();
  }

  @Override
  public ProcessInstanceBatchState valueLifeCycle() {
    return ProcessInstanceBatchState.ACTIVATE_TASK_TERMINATE;
  }

  @Override
  public void processRecord(final BusinessLogRecord<ProcessInstanceBatchRecord> record) {
    final ProcessInstanceBatchRecord value = record.getValue();
    final List<UserTaskRecord> inFlight = collectInFlight(value);
    if (!inFlight.isEmpty()) {
      final UserTaskRecord first = inFlight.getFirst();
      // 任务命令透传外层引用（非本批 id）：若名下任务终止链先于批收尾推进活动终止，
      // 活动 onTerminated 的批回报才能正确指向外层批（本批无行可查）
      writer.addCommand(
          first.getTaskId(),
          UserTaskLifeCycle.TERMINATING,
          record.getRequestId(),
          value.getBatchAfterOperationReference(),
          value.getBatchAfterBatchOperationReference(),
          first);
      if (inFlight.size() > 1) {
        value.setCurrentExecutionId(first.getTaskId());
        writer.addCommand(
            value.getBatchId(),
            ProcessInstanceBatchState.ACTIVATE_TASK_TERMINATE,
            record.getRequestId(),
            value);
        return;
      }
    }
    // 名下在途已清（或本就无在途，如 AWAITING 下原任务已完成）：批终态 + batchAfter 续活动终止链；
    // 活动行已不在说明终止链已先行推进（任务侧携带外层引用），不重复发
    if (activityInstance.getRecord(value.getBatchAfterBusinessId()) != null) {
      writer.addCommand(
          value.getBatchAfterBusinessId(),
          value.getBatchAfterLifeCycle(),
          value.getRequestId(),
          value.getBatchAfterOperationReference(),
          value.getBatchAfterBatchOperationReference(),
          batchBehavior.getBatchAfterRecord(value));
    }
    writer.addEvent(
        record.getKey(), ProcessInstanceBatchState.BATCH_COMPLETED, record.getRequestId(), value);
  }

  /** 收集活动名下在途任务：主体为组活动实例时只收组员（taskId 字段是锚任务非组员）；普通活动收原任务 + 组员关联，按 taskId 升序、跳过游标已处理者 */
  private List<UserTaskRecord> collectInFlight(final ProcessInstanceBatchRecord value) {
    final List<UserTaskRecord> tasks = new ArrayList<>();
    final ActivityInstanceRecord instance = activityInstance.getRecord(value.getBatchBusinessId());
    final boolean isGroup =
        instance != null
            && instance.getActivityDefinitionType() == BpmnElementType.ADD_SIGN_USER_TASK;
    if (instance != null && instance.getTaskId() > 0 && !isGroup) {
      final UserTaskRecord origin = userTask.getRecord(instance.getTaskId());
      if (origin != null && origin.getTaskId() > value.getCurrentExecutionId()) {
        tasks.add(origin);
      }
    }
    userTask
        .getAddSignTasks(value.getBatchBusinessId())
        .forEach(
            task -> {
              if (task.getTaskId() > value.getCurrentExecutionId()) {
                tasks.add(task);
              }
            });
    tasks.sort(Comparator.comparingLong(UserTaskRecord::getTaskId));
    return tasks;
  }
}
