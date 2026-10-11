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

import static com.anyilanxin.kunpeng.gateway.grpc.utils.RequestUtil.ensureJsonSet;
import static com.anyilanxin.kunpeng.gateway.rest.core.BrokerCall.awaitAck;

import com.anyilanxin.kunpeng.broker.client.business.client.BusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.business.client.VariableClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.variable.request.VariableRemoveRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.variable.request.VariableUpdateRequest;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.VariableRemoveDto;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.VariableUpdateDto;
import com.anyilanxin.kunpeng.gateway.rest.service.VariableService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 变量业务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Service
public class VariableServiceImpl implements VariableService {
  private final VariableClient variable;

  public VariableServiceImpl(final BusinessCommandClient clients) {
    this.variable = clients.variable();
  }

  @Override
  public Mono<Void> update(final VariableUpdateDto dto) {
    final VariableUpdateRequest request = new VariableUpdateRequest();
    request
        .setVariable(ensureJsonSet(dto.variable()))
        .setProcessInstanceId(dto.processInstanceId())
        .setActivityInstanceId(dto.activityInstanceId())
        .setTaskId(dto.taskId());
    return awaitAck(variable.updateVariable(request));
  }

  @Override
  public Mono<Void> remove(final VariableRemoveDto dto) {
    final VariableRemoveRequest request = new VariableRemoveRequest();
    request
        .setRemoveVariable(dto.removeVariable())
        .setProcessInstanceId(dto.processInstanceId())
        .setActivityInstanceId(dto.activityInstanceId())
        .setTaskId(dto.taskId());
    return awaitAck(variable.removeVariable(request));
  }
}
