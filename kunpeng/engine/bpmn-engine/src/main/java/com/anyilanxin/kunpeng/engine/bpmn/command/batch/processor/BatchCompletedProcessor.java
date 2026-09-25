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
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BatchBehavior;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.batch.ImmutableBatchRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import java.util.Optional;

/**
 * 取消流程实例批处理
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BatchCompletedProcessor extends AbstractBatchProcessor {
  private final ImmutableBatchRepository batch;
  private final BatchBehavior batchBehavior;
  private final ImmutableProcessInstanceRepository processInstance;
  private final ImmutableActivityInstanceRepository activityInstance;

  public BatchCompletedProcessor(final LogEventWriter writer) {
    super(writer);
    final ImmutableBusinessRepository repository = writer.getRepository();
    batch = repository.batchRepository();
    processInstance = repository.processInstanceRepository();
    activityInstance = repository.instanceRepository();
    batchBehavior = writer.behavior().batchBehavior();
  }

  @Override
  public ProcessInstanceBatchState valueLifeCycle() {
    return ProcessInstanceBatchState.BATCH_COMPLETE;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void processRecord(final BusinessLogRecord<ProcessInstanceBatchRecord> record) {
    final ProcessInstanceBatchRecord value = record.getValue();
    if (!activityInstance.haveChildRecord(value.getBatchBusinessId())) {
      final Optional<ProcessInstanceBatchRecord> query = batch.query(record.getKey());
      if (query.isPresent()) {
        writer.addEvent(
            record.getKey(),
            ProcessInstanceBatchState.BATCH_COMPLETED,
            record.getRequestId(),
            value);

        writer.addCommand(
            value.getBatchAfterBusinessId(),
            value.getBatchAfterLifeCycle(),
            value.getRequestId(),
            value.getBatchAfterOperationReference(),
            value.getBatchAfterBatchOperationReference(),
            batchBehavior.getBatchAfterRecord(value));
      }
    }
  }
}
