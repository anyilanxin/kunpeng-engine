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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi;

import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.deployment.DeploymentApiRecordRegister;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.empty.EmptyRecordRegister;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.incident.IncidentApiRecordRegister;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.JobApiRecordRegister;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.message.MessageApiRecordRegister;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processdefinition.ProcessDefinitionApiRecordRegister;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.ProcessInstanceApiRecordRegister;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.signal.SignalApiRecordRegister;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.UserTaskApiRecordRegister;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.VariableApiRecordRegister;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapperRegister;

/**
 * 业务协议 API Record 总注册器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class CommandApiRecordRegister {
  private CommandApiRecordRegister() {}

  public static void register(final RecordValueMapperRegister valueMapper) {
    DeploymentApiRecordRegister.register(valueMapper);
    IncidentApiRecordRegister.register(valueMapper);
    JobApiRecordRegister.register(valueMapper);
    MessageApiRecordRegister.register(valueMapper);
    SignalApiRecordRegister.register(valueMapper);
    ProcessDefinitionApiRecordRegister.register(valueMapper);
    ProcessInstanceApiRecordRegister.register(valueMapper);
    UserTaskApiRecordRegister.register(valueMapper);
    EmptyRecordRegister.register(valueMapper);
    VariableApiRecordRegister.register(valueMapper);
  }
}
