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
package com.anyilanxin.kunpeng.broker.client.business.commandapi.signal.request;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.signal.SignalAbstractRequest;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.signal.correlation.SignalCorrelationRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.signal.CommandApiSignalValueLifeCycle;
import java.util.Map;
import org.agrona.DirectBuffer;
import org.apache.commons.lang3.StringUtils;

/**
 * 信号关联（广播触发订阅）请求。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SignalCorrelationRequest
    extends SignalAbstractRequest<SignalCorrelationRequestRecord> {

  public SignalCorrelationRequest(final String signalName) {
    super(CommandApiSignalValueLifeCycle.CORRELATION_REQUEST, new SignalCorrelationRequestRecord());
    getValue().setSignalName(signalName);
  }

  public SignalCorrelationRequest setProcessInstanceId(final long processInstanceId) {
    getValue().setProcessInstanceId(processInstanceId);
    setKey(processInstanceId);
    return this;
  }

  public SignalCorrelationRequest setTenantId(final String tenantId) {
    if (StringUtils.isNotBlank(tenantId)) {
      getValue().setTenantId(tenantId);
    }
    return this;
  }

  public SignalCorrelationRequest setVariable(final Map<String, Object> variables) {
    getValue().setVariables(variables);
    return this;
  }

  public SignalCorrelationRequest setVariable(final DirectBuffer variables) {
    getValue().setVariables(variables);
    return this;
  }
}
