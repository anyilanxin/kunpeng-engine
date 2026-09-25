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
package com.anyilanxin.kunpeng.broker.client.business.commandapi.variable.request;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.variable.VariableAbstractRequest;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.update.VariableUpdateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.variable.CommandApiVariableValueLifeCycle;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 变量更新请求。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class VariableUpdateRequest extends VariableAbstractRequest<VariableUpdateRequestRecord> {

  public VariableUpdateRequest() {
    super(CommandApiVariableValueLifeCycle.UPDATE_REQUEST, new VariableUpdateRequestRecord());
  }

  public VariableUpdateRequest setProcessInstanceId(final long processInstanceId) {
    getValue().setProcessInstanceId(processInstanceId);
    if (processInstanceId > 0) {
      setKey(processInstanceId);
    }
    return this;
  }

  public VariableUpdateRequest setActivityInstanceId(final long activityInstanceId) {
    getValue().setActivityInstanceId(activityInstanceId);
    if (activityInstanceId > 0) {
      setKey(activityInstanceId);
    }
    return this;
  }

  public VariableUpdateRequest setTaskId(final long taskId) {
    getValue().setTaskId(taskId);
    if (taskId > 0) {
      setKey(taskId);
    }
    return this;
  }

  public VariableUpdateRequest setVariable(final Map<String, Object> variables) {
    getValue().setVariables(variables);
    return this;
  }

  public VariableUpdateRequest setVariable(final DirectBuffer variables) {
    getValue().setVariables(variables);
    return this;
  }
}
