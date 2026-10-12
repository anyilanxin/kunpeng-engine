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
import static com.anyilanxin.kunpeng.gateway.rest.core.BrokerCall.awaitValue;

import com.anyilanxin.kunpeng.broker.client.business.client.BusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.business.client.ProcessInstanceClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request.ProcessInstanceCancelRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request.ProcessInstanceCreateRequest;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.ProcessInstanceCreateDto;
import com.anyilanxin.kunpeng.gateway.rest.core.NotImplementedException;
import com.anyilanxin.kunpeng.gateway.rest.service.ProcessInstanceService;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.ProcessInstanceVo;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 流程实例业务实现：创建/取消经标准命令通道进 raft。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Service
public class ProcessInstanceServiceImpl implements ProcessInstanceService {
  private final ProcessInstanceClient processInstance;

  public ProcessInstanceServiceImpl(final BusinessCommandClient clients) {
    this.processInstance = clients.processInstance();
  }

  @Override
  public Mono<ProcessInstanceVo> create(final ProcessInstanceCreateDto dto) {
    // 定义定位二选一：id 与 key 不可同时缺省或同时提供
    final boolean byId = dto.processDefinitionId() != null && dto.processDefinitionId() > 0;
    final boolean byKey =
        dto.processDefinitionKey() != null && !dto.processDefinitionKey().isEmpty();
    if (byId == byKey) {
      return Mono.error(
          new IllegalArgumentException("processDefinitionKey 与 processDefinitionId 必须二选一"));
    }
    final ProcessInstanceCreateRequest request = new ProcessInstanceCreateRequest();
    if (byId) {
      request.setProcessDefinitionId(dto.processDefinitionId());
    } else {
      // version 缺省 -1 表示最新版本
      request.setProcessDefinitionKey(
          dto.processDefinitionKey(), dto.version() == null ? -1 : dto.version());
    }
    request.setVariable(ensureJsonSet(dto.variable())).setTenantId(dto.tenantId());
    return awaitValue(processInstance.createProcessInstance(request), ProcessInstanceVo::of);
  }

  @Override
  public Mono<ProcessInstanceVo> createWithResult(final ProcessInstanceCreateDto dto) {
    return Mono.error(new NotImplementedException("createProcessInstanceWithResult：引擎侧尚无支撑"));
  }

  @Override
  public Mono<Void> cancel(final long processInstanceKey) {
    return awaitAck(
        processInstance.cancelProcessInstance(
            new ProcessInstanceCancelRequest(processInstanceKey)));
  }
}
