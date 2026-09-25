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
package com.anyilanxin.kunpeng.engine.bpmn.command.batch.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.batch.AbstractBatchProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.repository.business.modules.batch.ImmutableBatchRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 取消流程实例批处理
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BatchTerminateExecutorProcessProcessor extends AbstractBatchProcessor {
  private static final Logger LOG =
      LoggerFactory.getLogger(BatchTerminateExecutorProcessProcessor.class);
  private final ImmutableBatchRepository batch;

  public BatchTerminateExecutorProcessProcessor(final LogEventWriter writer) {
    super(writer);
    batch = writer.getRepository().batchRepository();
  }

  @Override
  public ProcessInstanceBatchState valueLifeCycle() {
    return ProcessInstanceBatchState.ACTIVATE_TERMINATE_PROCESS;
  }

  @Override
  public void processRecord(final BusinessLogRecord<ProcessInstanceBatchRecord> record) {
    final ProcessInstanceBatchRecord value = record.getValue();
    final List<Long> childRecord =
        activityInstance.getChildRecord(
            value.getBatchBusinessId(),
            2,
            v ->
                (v.getLifeCycle() == ActivityInstanceLifeCycle.ACTIVATED
                        || v.getLifeCycle() == ActivityInstanceLifeCycle.ACTIVATING
                        || v.getLifeCycle() == ActivityInstanceLifeCycle.TAKING)
                    && v.getActivityInstanceId() > value.getCurrentExecutionId());
    LOG.debug("childRecord size {}", childRecord.size());
    if (!childRecord.isEmpty()) {
      // 4. 触发直接子任务终止
      final Long firstKey = childRecord.getFirst();
      final ActivityInstanceRecord first = activityInstance.getRecord(firstKey);
      writer.addCommand(
          first.getActivityInstanceId(),
          ActivityInstanceLifeCycle.TERMINATING,
          record.getRequestId(),
          record.getKey(),
          value.getBatchId(),
          first);
      // 5. 如果直接子任务大于1个，那就在调起第二次批处理
      if (childRecord.size() > 1) {
        value.setCurrentExecutionId(first.getActivityInstanceId());
        writer.addCommand(
            value.getBatchId(),
            ProcessInstanceBatchState.ACTIVATE_TERMINATE_PROCESS,
            record.getRequestId(),
            value);
      }
    }
  }
}
