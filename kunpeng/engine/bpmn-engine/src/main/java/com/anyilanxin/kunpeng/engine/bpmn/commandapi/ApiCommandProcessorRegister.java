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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessors;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.deployment.DeploymentApiProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.incident.IncidentApiProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.job.JobApiProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.message.MessageCorrelationApiProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.processdefinition.ProcessDefinitionApiProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.processinstance.ProcessInstanceApiProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.signal.SignalCorrelationApiProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.UserTaskApiProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.variable.VariableApiProcessorRegister;

/**
 * 引擎 API 命令处理器总注册器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ApiCommandProcessorRegister {

  public static void registerRepository(
      final LogEventProcessors processors, final LogEventWriter writer) {
    DeploymentApiProcessorRegister.registerRepository(processors, writer);
    IncidentApiProcessorRegister.registerRepository(processors, writer);
    ProcessDefinitionApiProcessorRegister.registerRepository(processors, writer);
    ProcessInstanceApiProcessorRegister.registerRepository(processors, writer);
    UserTaskApiProcessorRegister.registerRepository(processors, writer);
    MessageCorrelationApiProcessorRegister.registerRepository(processors, writer);
    SignalCorrelationApiProcessorRegister.registerRepository(processors, writer);
    JobApiProcessorRegister.registerRepository(processors, writer);
    VariableApiProcessorRegister.registerRepository(processors, writer);
  }
}
