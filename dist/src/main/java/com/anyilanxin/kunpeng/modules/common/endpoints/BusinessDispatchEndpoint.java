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
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * 业务面调度查询 Actuator 端点（/actuator/businessDispatch）：查询业务集群元数据。
 *
 * @author zxuanhong
 * @since
 */
@Endpoint(id = "businessDispatch")
public class BusinessDispatchEndpoint {
  private final BusinessDispatchClient dispatchClient;

  public BusinessDispatchEndpoint(final BusinessDispatchClient dispatchClient) {
    this.dispatchClient = dispatchClient;
  }

  /** 查询业务集群元数据 */
  @ReadOperation
  public DispatchResult dispatchQuery() {
    return BusinessDispatchResponses.await(dispatchClient.businessDispatchQuery());
  }
}
