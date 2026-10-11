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

import com.anyilanxin.kunpeng.broker.client.business.client.DeploymentClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.deployment.request.DeploymentCreateRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.deployment.request.DeploymentDeleteRequest;
import com.anyilanxin.kunpeng.broker.client.core.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponse;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.deployment.create.DeploymentCreateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.deployment.delete.DeploymentDeleteResponseRecord;
import java.util.concurrent.CompletableFuture;

/**
 * 部署域命令客户端实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DefaultDeploymentClient implements DeploymentClient {
  private final BrokerClient brokerClient;

  public DefaultDeploymentClient(final BrokerClient brokerClient) {
    this.brokerClient = brokerClient;
  }

  @Override
  public CompletableFuture<BrokerResponse<DeploymentCreateResponseRecord>> deployResource(
      final DeploymentCreateRequest request) {
    return brokerClient.sendRequest(request);
  }

  @Override
  public CompletableFuture<BrokerResponse<DeploymentDeleteResponseRecord>> deleteResource(
      final DeploymentDeleteRequest request) {
    return brokerClient.sendRequest(request);
  }
}
