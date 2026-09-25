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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.job.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.job.JobApiBatchAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.activate.JobBatchActivateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobBatchLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.CommandApiJobBatchValueLifeCycle;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import com.anyilanxin.kunpeng.structpack.value.ValueArray;
import java.time.InstantSource;
import org.agrona.DirectBuffer;

/**
 * job 批量激活（拉取）API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobBatchActivateApiProcessor
    extends JobApiBatchAbstractProcessor<JobBatchActivateRequestRecord> {
  final JobBatchRecord jobBatchRecord = new JobBatchRecord();
  private final InstantSource clock;

  public JobBatchActivateApiProcessor(final LogEventWriter writer) {
    super(writer);
    clock = writer.clock();
  }

  @Override
  public CommandApiJobBatchValueLifeCycle valueLifeCycle() {
    return CommandApiJobBatchValueLifeCycle.ACTIVATE_REQUEST;
  }

  @Override
  public void processRecord(final BusinessLogRecord<JobBatchActivateRequestRecord> record) {
    final JobBatchActivateRequestRecord value = record.getValue();
    jobBatchRecord.reset();
    jobBatchRecord.setBatchJobId(writer.nextCurrentSourceKey());
    jobBatchRecord.setMaxJobsActivate(value.getMaxJobsActivate());
    jobBatchRecord.setDeadline(clock.millis() + value.getTimeout());
    jobBatchRecord.setWorker(value.getWorker());
    jobBatchRecord.setTenantIds(value.getTenantIds());
    jobBatchRecord.setJobType(value.getJobTypeBuffer());
    final ValueArray<StringValue> variables = jobBatchRecord.variables();
    for (final StringValue variable : value.variables()) {
      final DirectBuffer directBuffer = variable.getValue();
      variables.add().wrap(directBuffer, 0, directBuffer.capacity());
    }
    writer.addCommand(JobBatchLifeCycle.ACTIVATE, record.getRequestId(), jobBatchRecord);
  }
}
