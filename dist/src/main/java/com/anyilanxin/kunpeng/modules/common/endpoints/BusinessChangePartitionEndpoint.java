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
 * 业务集群分区数量修改 Actuator 端点（/actuator/businessChangePartition）：apply=false 仅生成调度计划预览。
 *
 * @author zxuanhong
 * @since
 */
@Endpoint(id = "businessChangePartition")
public class BusinessChangePartitionEndpoint {
  private final BusinessDispatchClient dispatchClient;

  public BusinessChangePartitionEndpoint(final BusinessDispatchClient dispatchClient) {
    this.dispatchClient = dispatchClient;
  }

  /** 修改业务集群分区数量：apply=false 仅生成调度计划预览 */
  @WriteOperation
  public DispatchResult changePartition(final int expectPartitionsCount, final boolean apply) {
    return BusinessDispatchResponses.await(
        dispatchClient.businessChangePartition(expectPartitionsCount, apply));
  }
}
