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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable;

import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.remove.VariableRemoveRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.remove.VariableRemoveResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.update.VariableUpdateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.update.VariableUpdateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapperRegister;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.variable.CommandApiVariableValueLifeCycle;

/**
 * 变量域 API Record 注册器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class VariableApiRecordRegister {
  private VariableApiRecordRegister() {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void register(final RecordValueMapperRegister valueMapper) {
    valueMapper
        .register(CommandApiVariableValueLifeCycle.DELETE_REQUEST, VariableRemoveRequestRecord::new)
        .register(
            CommandApiVariableValueLifeCycle.DELETE_RESPONSE, VariableRemoveResponseRecord::new)
        .register(CommandApiVariableValueLifeCycle.UPDATE_REQUEST, VariableUpdateRequestRecord::new)
        .register(
            CommandApiVariableValueLifeCycle.UPDATE_RESPONSE, VariableUpdateResponseRecord::new);
  }
}
