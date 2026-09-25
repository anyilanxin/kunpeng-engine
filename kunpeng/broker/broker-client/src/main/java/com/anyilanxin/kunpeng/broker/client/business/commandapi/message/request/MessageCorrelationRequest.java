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
package com.anyilanxin.kunpeng.broker.client.business.commandapi.message.request;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.message.MessageAbstractRequest;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.message.correlation.MessageCorrelationRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.message.CommandApiMessageValueLifeCycle;
import java.util.Map;
import org.agrona.DirectBuffer;
import org.apache.commons.lang3.StringUtils;

/**
 * 消息关联（发布并触发订阅）请求。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class MessageCorrelationRequest
    extends MessageAbstractRequest<MessageCorrelationRequestRecord> {

  public MessageCorrelationRequest(final String messageName) {
    super(
        CommandApiMessageValueLifeCycle.CORRELATION_REQUEST, new MessageCorrelationRequestRecord());
    getValue().setMessageName(messageName);
  }

  public MessageCorrelationRequest setCorrelationKey(final String correlationKey) {
    if (StringUtils.isNotBlank(correlationKey)) {
      getValue().setCorrelationKey(correlationKey);
    }
    return this;
  }

  public MessageCorrelationRequest setProcessInstanceId(final long processInstanceId) {
    getValue().setProcessInstanceId(processInstanceId);
    setKey(processInstanceId);
    return this;
  }

  public MessageCorrelationRequest setTenantId(final String tenantId) {
    if (StringUtils.isNotBlank(tenantId)) {
      getValue().setTenantId(tenantId);
    }
    return this;
  }

  public MessageCorrelationRequest setVariable(final Map<String, Object> variables) {
    getValue().setVariables(variables);
    return this;
  }

  public MessageCorrelationRequest setVariable(final DirectBuffer variables) {
    getValue().setVariables(variables);
    return this;
  }
}
