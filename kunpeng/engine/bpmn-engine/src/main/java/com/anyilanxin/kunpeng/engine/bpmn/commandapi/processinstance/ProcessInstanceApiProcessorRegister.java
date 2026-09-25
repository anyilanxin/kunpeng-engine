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

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessors;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.processinstance.processor.ProcessInstanceActivateAndResultProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.processinstance.processor.ProcessInstanceCancelProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.processinstance.processor.ProcessInstanceCreateProcessor;

/**
 * 流程实例相关
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ProcessInstanceApiProcessorRegister {

  public static void registerRepository(
      final LogEventProcessors processors, final LogEventWriter writer) {
    processors
        .onCommand(new ProcessInstanceCreateProcessor(writer))
        .onCommand(new ProcessInstanceActivateAndResultProcessor(writer))
        .onCommand(new ProcessInstanceCancelProcessor(writer));
  }
}
