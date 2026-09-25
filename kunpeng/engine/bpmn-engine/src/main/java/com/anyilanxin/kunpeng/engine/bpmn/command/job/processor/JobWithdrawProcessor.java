/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * job 回退命令处理：流推送未送达（流断/网关写失败）时由 broker 侧追加 WITHDRAW 命令， 写 TIMED_OUT 事件回待激活
 * （READY），与到期回退走同一存储迁移——job 可立即被其他流或长轮询取走， 不必等满一个 deadline 周期。
 *
 * <p>幂等防御与 {@link JobTimeOutProcessor} 一致：job 已不存在（期间已完成）或未激活时静默跳过。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobWithdrawProcessor extends JobAbstractProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(JobWithdrawProcessor.class);

  public JobWithdrawProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public JobLifeCycle processState() {
    return JobLifeCycle.WITHDRAW;
  }

  @Override
  public void processRecord(final BusinessLogRecord<JobRecord> logRecord) {
    final long jobKey = logRecord.getKey();
    final JobRecord value = logRecord.getValue();
    if (value.getJobId() <= 0 || value.getDueDate() <= 0) {
      LOG.debug("回退命令跳过，job 已不在激活态 [jobKey: {}]", jobKey);
      return;
    }
    writer.addEvent(jobKey, JobLifeCycle.TIMED_OUT, logRecord.getRequestId(), value);
  }
}
