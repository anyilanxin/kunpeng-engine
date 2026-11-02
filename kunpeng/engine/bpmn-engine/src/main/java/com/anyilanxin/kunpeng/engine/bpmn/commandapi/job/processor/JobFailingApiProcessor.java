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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.job.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.job.JobApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.fail.JobFailRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.fail.JobFailResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.CommandApiJobValueLifeCycle;

/**
 * job 失败 API 处理器（客户端 fail 链的引擎编排）。
 *
 * <p>契约（workjob §6）：重试是 retries 计数 + 回 READY 重派（任何消费者可再取），不依赖 worker。 retries&gt;0 复用
 * WITHDRAW→TIMED_OUT 的回 READY 存储迁移（摘激活列族与 deadline 索引、清
 * lockOwner/dueDate，retries/retryBackOff/deniedReason 随事件值落库）；retries=0 走 REFUSING 终态拒绝
 * （JobRefuseProcessor：REFUSED 事件 + 按 jobKind 回发活动/监听器拒绝链）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobFailingApiProcessor extends JobApiAbstractProcessor<JobFailRequestRecord> {
  private final JobFailResponseRecord response = new JobFailResponseRecord();

  public JobFailingApiProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public CommandApiJobValueLifeCycle valueLifeCycle() {
    return CommandApiJobValueLifeCycle.FAIL_REQUEST;
  }

  @Override
  public void innerProcessRecord(
      final BusinessLogRecord<JobFailRequestRecord> record, final JobRecord jobRecord) {
    final JobFailRequestRecord value = record.getValue();
    jobRecord.setRetries(value.getRetries());
    jobRecord.setRetryBackOff(value.getRetryBackOff());
    jobRecord.setDeniedReason(value.getDeniedReasonBuffer());
    // 注册延迟响应
    response.reset();
    response.setJobId(value.getJobId());
    writer.adResponse(CommandApiJobValueLifeCycle.FAIL_RESPONSE, record.getRequestId(), response);
    if (value.getRetries() > 0) {
      // 重试：回 READY 重派（与推送失败回退、到期回退同一存储迁移）
      writer.addCommand(
          jobRecord.getJobId(), JobLifeCycle.WITHDRAW, record.getRequestId(), jobRecord);
    } else {
      // 重试耗尽：终态拒绝
      writer.addCommand(
          jobRecord.getJobId(), JobLifeCycle.REFUSING, record.getRequestId(), jobRecord);
    }
  }
}
