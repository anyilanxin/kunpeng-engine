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
package com.anyilanxin.kunpeng.broker.client.admin;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.dispatch.BusinessChangeResponseRecord;
import java.util.concurrent.CompletableFuture;

/**
 * @author zxuanhong
 * @since
 */
public interface BusinessDispatchClient {
  /** 集群重新负载 */
  CompletableFuture<BrokerResponse<BusinessChangeResponseRecord>> businessClusterBalance(
      boolean apply);

  /** 集群修改分区 */
  CompletableFuture<BrokerResponse<BusinessChangeResponseRecord>> businessChangePartition(
      int expectPartitionsCount, boolean apply);

  /** 集群修改副本数量 */
  CompletableFuture<BrokerResponse<BusinessChangeResponseRecord>> businessChangeReplication(
      int expectReplicationFactor, boolean apply);

  /** 查询集群元数据 */
  CompletableFuture<BrokerResponse<BusinessChangeResponseRecord>> businessDispatchQuery();
}
