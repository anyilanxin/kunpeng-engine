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
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.job.JobApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.complete.JobCompleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.complete.JobCompleteResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobKindType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.CommandApiJobValueLifeCycle;

/**
 * job 完成 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobCompletingApiProcessor extends JobApiAbstractProcessor<JobCompleteRequestRecord> {
  private final JobCompleteResponseRecord response = new JobCompleteResponseRecord();

  public JobCompletingApiProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public CommandApiJobValueLifeCycle valueLifeCycle() {
    return CommandApiJobValueLifeCycle.COMPLETE_REQUEST;
  }

  @Override
  public void innerProcessRecord(
      final BusinessLogRecord<JobCompleteRequestRecord> record, final JobRecord jobRecord) {
    final JobCompleteRequestRecord value = record.getValue();
    jobRecord.setVariables(value.getVariablesBuffer());
    jobRecord.setLocalVariables(value.getLocalVariablesBuffer());
    // 注册延迟响应
    final JobKindType jobKind = jobRecord.getJobKind();
    response.reset();
    response.setJobId(value.getJobId());
    writer.adResponse(
        CommandApiJobValueLifeCycle.COMPLETE_RESPONSE, record.getRequestId(), response);
    // 处理业务
    writer.addCommand(
        jobRecord.getJobId(), JobLifeCycle.COMPLETING, record.getRequestId(), jobRecord);
  }
}
