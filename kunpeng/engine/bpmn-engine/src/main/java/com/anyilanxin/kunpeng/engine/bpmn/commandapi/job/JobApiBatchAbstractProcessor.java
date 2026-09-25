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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.job;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessorSingleState;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.CommandApiJobBatchValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.ImmutableJobRepository;

/**
 * job 批量 API 处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class JobApiBatchAbstractProcessor<T extends UnifiedRecordValue>
    implements LogEventProcessorSingleState<T> {
  protected final ImmutableJobRepository job;
  protected final LogEventWriter writer;

  public JobApiBatchAbstractProcessor(final LogEventWriter writer) {
    final ImmutableBusinessRepository repository = writer.getRepository();
    job = repository.jobRepository();
    this.writer = writer;
  }

  @Override
  public ValueType valueType() {
    return ValueType.JOB_BATCH_API;
  }

  @Override
  public abstract CommandApiJobBatchValueLifeCycle valueLifeCycle();
}
