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

import static com.anyilanxin.kunpeng.gateway.rest.core.BrokerCall.awaitAck;

import com.anyilanxin.kunpeng.broker.client.business.client.BusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.business.client.IncidentClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.incident.request.IncidentResolveRequest;
import com.anyilanxin.kunpeng.gateway.rest.service.IncidentService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 事件（incident）业务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Service
public class IncidentServiceImpl implements IncidentService {
  private final IncidentClient incident;

  public IncidentServiceImpl(final BusinessCommandClient clients) {
    this.incident = clients.incident();
  }

  @Override
  public Mono<Void> resolve(final long incidentKey, final String tenantId) {
    final IncidentResolveRequest request = new IncidentResolveRequest(incidentKey);
    request.setTenantId(tenantId);
    return awaitAck(incident.resolveIncident(request));
  }
}
