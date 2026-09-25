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
package com.anyilanxin.kunpeng.engine.bpmn.command.batch;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessorSingleState;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;

/**
 * 批量命令处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class AbstractBatchProcessor
    implements LogEventProcessorSingleState<ProcessInstanceBatchRecord> {
  protected final LogEventWriter writer;
  protected final ImmutableProcessInstanceRepository processInstance;
  protected final ImmutableActivityInstanceRepository activityInstance;

  public AbstractBatchProcessor(final LogEventWriter writer) {
    this.writer = writer;
    processInstance = writer.getRepository().processInstanceRepository();
    activityInstance = writer.getRepository().instanceRepository();
  }

  @Override
  public ValueType valueType() {
    return ValueType.PROCESS_BATCH_INSTANCE;
  }

  @Override
  public abstract ProcessInstanceBatchState valueLifeCycle();
}
