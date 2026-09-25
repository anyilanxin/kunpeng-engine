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
package com.anyilanxin.kunpeng.engine.bpmn.command;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessors;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.batch.BatchProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.BpmnElementProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.command.deployment.DeploymentProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.command.distribute.DistributeProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.command.historycleanup.HistoryCleanupProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.command.incident.IncidentProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.command.job.JobProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.command.message.MessageProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.command.processdefinition.ProcessDefinitionProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.command.signal.SignalProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.command.timer.TimerProcessorRegister;
import com.anyilanxin.kunpeng.engine.bpmn.command.variable.VariableProcessorRegister;

/**
 * 引擎命令处理器总注册器：登记全部命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class CommandProcessorRegister {

  public static void registerRepository(
      final LogEventProcessors processors, final LogEventWriter writer) {
    DistributeProcessorRegister.registerRepository(processors, writer);
    BatchProcessorRegister.registerRepository(processors, writer);
    BpmnElementProcessorRegister.registerRepository(processors, writer);
    ProcessDefinitionProcessorRegister.registerRepository(processors, writer);
    JobProcessorRegister.registerRepository(processors, writer);
    IncidentProcessorRegister.registerRepository(processors, writer);
    MessageProcessorRegister.registerRepository(processors, writer);
    SignalProcessorRegister.registerRepository(processors, writer);
    TimerProcessorRegister.registerRepository(processors, writer);
    HistoryCleanupProcessorRegister.registerRepository(processors, writer);
    DeploymentProcessorRegister.registerRepository(processors, writer);
    VariableProcessorRegister.registerRepository(processors, writer);
  }
}
