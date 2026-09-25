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
package com.anyilanxin.kunpeng.engine.bpmn.command.job;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobBatchLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.ImmutableJobRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.ImmutableUserTaskRepository;

/**
 * job 批量命令处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class JobBatchAbstractProcessor implements LogEventProcessor<JobBatchRecord> {
  protected final LogEventWriter writer;
  protected final ImmutableJobRepository job;
  protected final ImmutableProcessInstanceRepository processInstance;
  protected final ImmutableUserTaskRepository userTask;
  protected final ImmutableActivityInstanceRepository activityInstance;

  public JobBatchAbstractProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    job = repository.jobRepository();
    processInstance = repository.processInstanceRepository();
    userTask = repository.userTaskRepository();
    activityInstance = repository.instanceRepository();
  }

  @Override
  public JobBatchLifeCycle[] valueLifeCycles() {
    return new JobBatchLifeCycle[] {processState()};
  }

  public abstract JobBatchLifeCycle processState();

  @Override
  public ValueType valueType() {
    return ValueType.JOB_BATCH;
  }
}
