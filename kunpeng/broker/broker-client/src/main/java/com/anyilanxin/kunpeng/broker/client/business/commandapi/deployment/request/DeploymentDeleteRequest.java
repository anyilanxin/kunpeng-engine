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
package com.anyilanxin.kunpeng.broker.client.business.commandapi.deployment.request;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.deployment.DeploymentAbstractRequest;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.deployment.delete.DeploymentDeleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.CommandApiDeploymentValueLifeCycle;

/**
 * 部署删除请求。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DeploymentDeleteRequest
    extends DeploymentAbstractRequest<DeploymentDeleteRequestRecord> {

  public DeploymentDeleteRequest() {
    super(CommandApiDeploymentValueLifeCycle.CREATE_REQUEST, new DeploymentDeleteRequestRecord());
    setPartitionId(DISTRIBUTE_PARTITION);
  }

  public DeploymentDeleteRequest setDeploymentId(final long deploymentId) {
    getValue().setDeploymentId(deploymentId);
    super.setKey(deploymentId);
    return this;
  }
}
