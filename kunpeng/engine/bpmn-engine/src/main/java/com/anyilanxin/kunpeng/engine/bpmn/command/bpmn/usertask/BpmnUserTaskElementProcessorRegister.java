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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessors;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.processor.*;

/**
 * 用户任务元素处理器注册器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnUserTaskElementProcessorRegister {

  private BpmnUserTaskElementProcessorRegister() {}

  public static void registerRepository(
      final LogEventProcessors processors, final LogEventWriter writer) {
    processors
        .onCommand(new BpmnListenerProcessor(writer))
        .onCommand(new BpmnUserTaskAssigneProcessor(writer))
        .onCommand(new BpmnUserTaskCanceledProcessor(writer))
        .onCommand(new BpmnUserTaskCancelProcessor(writer))
        .onCommand(new BpmnUserTaskClaimProcessor(writer))
        .onCommand(new BpmnUserTaskCompletedProcessor(writer))
        .onCommand(new BpmnUserTaskCompletingProcessor(writer))
        .onCommand(new BpmnUserTaskCreatedProcessor(writer))
        .onCommand(new BpmnUserTaskCreatingProcessor(writer))
        .onCommand(new BpmnUserTaskDeletedProcessor(writer))
        .onCommand(new BpmnUserTaskDeleteProcessor(writer))
        .onCommand(new BpmnUserTaskOwnProcessor(writer))
        .onCommand(new BpmnUserTaskResolveProcessor(writer))
        .onCommand(new BpmnUserTaskTerminaingProcessor(writer))
        .onCommand(new BpmnUserTaskTerminatedProcessor(writer))
        .onCommand(new BpmnUserTaskUpdatedProcessor(writer))
        .onCommand(new BpmnUserTaskUpdatingProcessor(writer));
  }
}
