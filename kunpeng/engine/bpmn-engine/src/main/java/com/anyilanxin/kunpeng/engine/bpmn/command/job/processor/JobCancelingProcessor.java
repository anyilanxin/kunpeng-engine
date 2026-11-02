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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * job 取消命令处理：活动终止链（临时子流程批终止/多实例批终止/边界中断等）取消在途 job 写 CANCELED 事件，由存储 applier 整行删除（连带 deadline
 * 索引与派发列族），被取消的 job 从 派发查询中自然消失。
 *
 * <p>幂等防御：job 行已不存在（期间已完成/已取消，行由 COMPLETED/CANCELED applier 删除）时静默跳过——
 * 在途去重注册表只保证命令不重发，不保证处理时状态仍符合预期。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobCancelingProcessor extends JobAbstractProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(JobCancelingProcessor.class);

  public JobCancelingProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public JobLifeCycle processState() {
    return JobLifeCycle.CANCELING;
  }

  @Override
  public void processRecord(final BusinessLogRecord<JobRecord> logRecord) {
    final long jobKey = logRecord.getKey();
    final Optional<JobRecord> storedResult = job.query(jobKey);
    if (storedResult.isEmpty()) {
      LOG.debug("Cancel command skipped, job no longer exists [jobKey: {}]", jobKey);
      return;
    }
    // 行存在即在途（落库字段只会有 CREATED/UPDATED；拒绝/超时路径无字段更新）——直接取消，
    // 行删除（COMPLETED/CANCELED）即终态，由上方存在性检查承担仲裁
    writer.addEvent(jobKey, JobLifeCycle.CANCELED, logRecord.getRequestId(), storedResult.get());
  }
}
