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
package com.anyilanxin.kunpeng.gateway.rest.service.impl;

import static com.anyilanxin.kunpeng.broker.client.core.ClientRequest.RANDOM_PARTITION;
import static com.anyilanxin.kunpeng.gateway.grpc.utils.RequestUtil.ensureJsonSet;
import static com.anyilanxin.kunpeng.gateway.rest.core.BrokerCall.awaitAck;

import com.anyilanxin.kunpeng.broker.client.business.client.BusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.business.client.SignalClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.signal.request.SignalCorrelationRequest;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.SignalCorrelationDto;
import com.anyilanxin.kunpeng.gateway.rest.service.SignalService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 信号广播业务实现：未定向（processInstanceId&lt;=0）时随机分区广播。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Service
public class SignalServiceImpl implements SignalService {
  private final SignalClient signal;

  public SignalServiceImpl(final BusinessCommandClient clients) {
    this.signal = clients.signal();
  }

  @Override
  public Mono<Void> correlate(final SignalCorrelationDto dto) {
    final SignalCorrelationRequest request = new SignalCorrelationRequest(dto.signalName());
    request
        .setTenantId(dto.tenantId())
        .setProcessInstanceId(dto.processInstanceId())
        .setVariable(ensureJsonSet(dto.variable()));
    if (dto.processInstanceId() <= 0) {
      request.setPartitionId(RANDOM_PARTITION);
    }
    return awaitAck(signal.correlateSignal(request));
  }
}
