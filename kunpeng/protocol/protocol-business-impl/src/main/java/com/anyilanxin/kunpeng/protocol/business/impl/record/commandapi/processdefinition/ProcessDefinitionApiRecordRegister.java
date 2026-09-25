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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processdefinition;

import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processdefinition.activate.ProcessDefinitionActivateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processdefinition.activate.ProcessDefinitionActivateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processdefinition.delete.ProcessDefinitionDeleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processdefinition.delete.ProcessDefinitionDeleteResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processdefinition.suspend.ProcessDefinitionSuspendRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processdefinition.suspend.ProcessDefinitionSuspendResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapperRegister;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processdefinition.CommandApiProcessDefinitionValueLifeCycle;

/**
 * 流程定义域 API Record 注册器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ProcessDefinitionApiRecordRegister {
  private ProcessDefinitionApiRecordRegister() {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void register(final RecordValueMapperRegister valueMapper) {
    valueMapper
        .register(
            CommandApiProcessDefinitionValueLifeCycle.ACTIVATE_REQUEST,
            ProcessDefinitionActivateRequestRecord::new)
        .register(
            CommandApiProcessDefinitionValueLifeCycle.ACTIVATE_RESPONSE,
            ProcessDefinitionActivateResponseRecord::new)
        .register(
            CommandApiProcessDefinitionValueLifeCycle.DELETE_REQUEST,
            ProcessDefinitionDeleteRequestRecord::new)
        .register(
            CommandApiProcessDefinitionValueLifeCycle.DELETE_RESPONSE,
            ProcessDefinitionDeleteResponseRecord::new)
        .register(
            CommandApiProcessDefinitionValueLifeCycle.SUSPEND_REQUEST,
            ProcessDefinitionSuspendRequestRecord::new)
        .register(
            CommandApiProcessDefinitionValueLifeCycle.SUSPEND_RESPONSE,
            ProcessDefinitionSuspendResponseRecord::new);
  }
}
