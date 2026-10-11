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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.client.business.client.impl;

import com.anyilanxin.kunpeng.broker.client.business.client.MessageClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.message.request.MessageCorrelationRequest;
import com.anyilanxin.kunpeng.broker.client.core.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponse;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.message.correlation.MessageCorrelationResponseRecord;
import java.util.concurrent.CompletableFuture;

/**
 * 消息域命令客户端实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DefaultMessageClient implements MessageClient {
  private final BrokerClient brokerClient;

  public DefaultMessageClient(final BrokerClient brokerClient) {
    this.brokerClient = brokerClient;
  }

  @Override
  public CompletableFuture<BrokerResponse<MessageCorrelationResponseRecord>> correlateMessage(
      final MessageCorrelationRequest request) {
    return brokerClient.sendRequest(request);
  }
}
