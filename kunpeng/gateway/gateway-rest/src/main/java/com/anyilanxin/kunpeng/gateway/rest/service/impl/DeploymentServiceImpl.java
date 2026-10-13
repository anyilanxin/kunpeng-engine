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
import static com.anyilanxin.kunpeng.gateway.rest.core.BrokerCall.awaitValue;

import com.anyilanxin.kunpeng.broker.client.business.client.BusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.business.client.DeploymentClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.deployment.request.DeploymentCreateRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.deployment.request.DeploymentDeleteRequest;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.DeploymentCreateDto;
import com.anyilanxin.kunpeng.gateway.rest.service.DeploymentService;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.DeploymentVo;
import java.util.Base64;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 部署管理业务实现：部署创建/删除经标准命令通道进 raft。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Service
public class DeploymentServiceImpl implements DeploymentService {
  private final DeploymentClient deployment;

  public DeploymentServiceImpl(final BusinessCommandClient clients) {
    this.deployment = clients.deployment();
  }

  @Override
  public Mono<DeploymentVo> deploy(final DeploymentCreateDto dto) {
    final DeploymentCreateRequest request = new DeploymentCreateRequest();
    if (dto.tenantId() != null && !dto.tenantId().isBlank()) {
      request.setTenantId(dto.tenantId());
    }
    for (final DeploymentCreateDto.DeploymentResourceDto resource : dto.resources()) {
      request.addResource(
          resource.name(),
          Base64.getDecoder().decode(resource.contentBase64()),
          resource.extendedData());
    }
    return awaitValue(deployment.deployResource(request), DeploymentVo::of);
  }

  @Override
  public Mono<Void> delete(final long deploymentKey) {
    final DeploymentDeleteRequest request =
        new DeploymentDeleteRequest().setDeploymentId(deploymentKey);
    return awaitAck(deployment.deleteResource(request));
  }
}
