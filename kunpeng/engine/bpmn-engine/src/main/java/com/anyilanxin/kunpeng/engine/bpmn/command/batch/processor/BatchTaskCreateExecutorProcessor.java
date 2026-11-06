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

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.batch.AbstractBatchProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;

/**
 * 并行加签创建批执行器：主体=组活动实例（G），每轮按 G 作用域载荷与游标（权威在 G 作用域，批可续跑） 创建一个组员任务并自排。
 *
 * <p>主体守卫：组实例必须在激活态（终止/完结即停）；游标到表尾即批收尾。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BatchTaskCreateExecutorProcessor extends AbstractBatchProcessor {
  private final AddSignBehavior addSignBehavior;

  public BatchTaskCreateExecutorProcessor(final LogEventWriter writer) {
    super(writer);
    addSignBehavior = writer.behavior().addSignBehavior();
  }

  @Override
  public ProcessInstanceBatchState valueLifeCycle() {
    return ProcessInstanceBatchState.ACTIVATE_TASK_CREATE;
  }

  @Override
  public void processRecord(final BusinessLogRecord<ProcessInstanceBatchRecord> record) {
    final ProcessInstanceBatchRecord value = record.getValue();
    final ActivityInstanceRecord group = activityInstance.getRecord(value.getBatchBusinessId());
    if (group == null || group.getLifeCycle() != ActivityInstanceLifeCycle.ACTIVATED) {
      // 主体守卫：组终止/完结即停（追加并入若旧批在途由其续跑，耗尽后门户再挂批续增量）
      writer.addEvent(
          record.getKey(), ProcessInstanceBatchState.BATCH_COMPLETED, record.getRequestId(), value);
      return;
    }
    final boolean hasMore = addSignBehavior.createNextSignTask(group, record.getRequestId());
    if (hasMore) {
      writer.addCommand(
          value.getBatchId(),
          ProcessInstanceBatchState.ACTIVATE_TASK_CREATE,
          record.getRequestId(),
          value);
      return;
    }
    writer.addEvent(
        record.getKey(), ProcessInstanceBatchState.BATCH_COMPLETED, record.getRequestId(), value);
  }
}
