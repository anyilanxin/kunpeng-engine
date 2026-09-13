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
package com.anyilanxin.kunpeng.cluster.manager.business.raft;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import java.util.concurrent.CompletableFuture;

/**
 * 业务分区 Raft 客户端服务，按目标成员（MemberId）发起集群负载平衡、分区扩缩容与副本增减等远程执行请求。
 *
 * @author zxuanhong
 * @since
 */
public interface BusinessRaftClientService {

  CompletableFuture<Void> clusterBalance(final MemberId memberId);

  CompletableFuture<Void> partitionScaleUp(final MemberId memberId, int expectScaleUpNum);

  CompletableFuture<Void> partitionScaleDown(final MemberId memberId, int expectScaleDownNum);

  CompletableFuture<Void> partitionAddReplication(
      final MemberId memberId, int expectAddReplicationNum);

  CompletableFuture<Void> partitionRemoveReplication(
      final MemberId memberId, int expectRemoveReplicationNum);
}
