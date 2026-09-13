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
package com.anyilanxin.kunpeng.modules.common.endpoints;

import com.anyilanxin.kunpeng.broker.client.admin.BusinessDispatchClient;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

/**
 * 业务集群重新负载 Actuator 端点（/actuator/businessClusterBalance）：apply=false 仅生成调度计划预览。
 *
 * @author zxuanhong
 * @since
 */
@Endpoint(id = "businessClusterBalance")
public class BusinessClusterBalanceEndpoint {
  private final BusinessDispatchClient dispatchClient;

  public BusinessClusterBalanceEndpoint(final BusinessDispatchClient dispatchClient) {
    this.dispatchClient = dispatchClient;
  }

  /** 业务集群重新负载：apply=false 仅生成调度计划预览 */
  @WriteOperation
  public DispatchResult clusterBalance(final boolean apply) {
    return BusinessDispatchResponses.await(dispatchClient.businessClusterBalance(apply));
  }
}
