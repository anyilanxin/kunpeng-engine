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
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BpmnJobDeliveryBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.job.JobBatchAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.activate.JobBatchActivateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.activate.JobInfoRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobBatchLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.CommandApiJobBatchValueLifeCycle;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ACTIVATE 命令处理（长轮询拉取路径，经标准命令通道进入）：扫描 READY 集合激活至多 maxJobsActivate 条， 写 ACTIVATED 事件落账（存储层切激活态并登记
 * deadline 到期索引），并把激活明细构造为 commandapi 响应返回拉取方。
 *
 * <p>命令在分区内串行处理，天然无并发取批竞态；扫描落空（无 READY）时返回空响应，网关长轮询继续挂起。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobBatchActivateProcessor extends JobBatchAbstractProcessor {
  private final JobBatchActivateResponseRecord response = new JobBatchActivateResponseRecord();
  private final BpmnJobDeliveryBehavior activationBehavior;

  public JobBatchActivateProcessor(final LogEventWriter writer) {
    super(writer);
    final Behavior behavior = writer.behavior();
    activationBehavior = behavior.activationBehavior();
  }

  @Override
  public JobBatchLifeCycle processState() {
    return JobBatchLifeCycle.ACTIVATE;
  }

  @Override
  public void processRecord(final BusinessLogRecord<JobBatchRecord> logRecord) {
    final JobBatchRecord jobBatchRecord = logRecord.getValue();
    final int maxJobsActivate = jobBatchRecord.getMaxJobsActivate();
    response.reset();

    final AtomicBoolean haveJob = new AtomicBoolean(false);
    job.processJobBatch(
        jobBatchRecord.getJobTypeBuffer(),
        jobBatchRecord.getTenantIds(),
        (jobKey, jobRecord) -> {
          haveJob.set(true);
          // 响应
          response.keys().add().setValue(jobKey);
          final JobInfoRecord record = response.jobs().add();
          record.wrap(jobRecord);
          record.setDeadline(jobBatchRecord.getTimeout());
          record.setWorker(jobBatchRecord.getWorker());
          activationBehavior.collectVariable(jobBatchRecord.variables(), record);
          // 匹配激活
          jobBatchRecord.jobKeys().add().setValue(jobKey);
          if (response.getJobKeys().size() >= maxJobsActivate) {
            response.setTruncated(true);
            return true;
          }
          return false;
        });
    if (haveJob.get()) {
      writer.addEvent(
          jobBatchRecord.getBatchJobId(),
          JobBatchLifeCycle.ACTIVATED,
          logRecord.getRequestId(),
          jobBatchRecord);
    }
    if (logRecord.getRequestId() >= 0) {
      writer.adResponse(
          CommandApiJobBatchValueLifeCycle.ACTIVATE_RESPONSE, logRecord.getRequestId(), response);
    }
  }
}
