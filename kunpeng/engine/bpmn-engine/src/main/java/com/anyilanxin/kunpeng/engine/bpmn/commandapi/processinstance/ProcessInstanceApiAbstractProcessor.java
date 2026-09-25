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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.processinstance;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessorSingleState;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.CommandApiProcessInstanceValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;

/**
 * 流程实例 API 处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class ProcessInstanceApiAbstractProcessor<T extends UnifiedRecordValue>
    implements LogEventProcessorSingleState<T> {
  protected final LogEventWriter writer;
  protected final ImmutableProcessInstanceRepository processInstance;
  protected final ImmutableActivityInstanceRepository activityInstance;
  protected final ImmutableBpmnResourceRepository bpmnResource;

  public ProcessInstanceApiAbstractProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    bpmnResource = repository.bpmnResourceRepository();
    processInstance = repository.processInstanceRepository();
    activityInstance = repository.instanceRepository();
  }

  @Override
  public ValueType valueType() {
    return ValueType.PROCESS_INSTANCE_API;
  }

  @Override
  public abstract CommandApiProcessInstanceValueLifeCycle valueLifeCycle();
}
