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
package com.anyilanxin.kunpeng.engine.bpmn.command.job.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.job.JobAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * job 超时命令处理：到期未 resolve 的已激活 job 写 TIMED_OUT 事件回待激活（READY）， 由存储 applier 清 deadline
 * 索引与激活痕迹，随后轮询/推送链路可重新消费。
 *
 * <p>幂等防御：job 已不存在（期间已完成/删除）或未激活（无 deadline）时静默跳过—— 在途去重注册表只保证命令不重发，不保证处理时状态仍符合预期。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobTimeOutProcessor extends JobAbstractProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(JobTimeOutProcessor.class);

  public JobTimeOutProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public JobLifeCycle processState() {
    return JobLifeCycle.TIME_OUT;
  }

  @Override
  public void processRecord(final BusinessLogRecord<JobRecord> logRecord) {
    final long jobKey = logRecord.getKey();
    final JobRecord value = logRecord.getValue();
    if (value.getJobId() <= 0 || value.getDueDate() <= 0) {
      LOG.debug("Timeout command skipped, job no longer activated [jobKey: {}]", jobKey);
      return;
    }
    writer.addEvent(jobKey, JobLifeCycle.TIMED_OUT, logRecord.getRequestId(), value);
  }
}
