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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.listener;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListenerEventType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnExecutionListener;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.BpmnProcessElementListenerProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceListenerType;
import java.util.List;

/**
 * 流程结束监听器处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnProcessElementEndListenerProcessor extends BpmnProcessElementListenerProcessor {

  public BpmnProcessElementEndListenerProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public ProcessInstanceListenerType getType() {
    return ProcessInstanceListenerType.END;
  }

  @Override
  public List<BpmnExecutionListener> getExecutionListeners(final BpmnProcess element) {
    return element.getExecutionListeners(KunpengExecutionListenerEventType.end);
  }

  @Override
  public void toCompleted(final ProcessInstanceRecord instanceRecord, final long requestId) {
    instanceRecord.setListenerType(ProcessInstanceListenerType.UNKNOW);
    instanceRecord.setListenerIndex(-1);
    writer.addCommand(
        instanceRecord.getProcessInstanceId(),
        ProcessInstanceLifeCycle.COMPLETED,
        requestId,
        instanceRecord);
  }
}
