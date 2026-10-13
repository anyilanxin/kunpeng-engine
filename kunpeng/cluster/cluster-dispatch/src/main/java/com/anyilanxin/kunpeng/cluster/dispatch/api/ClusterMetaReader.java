/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.cluster.dispatch.api;

import static com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterMetaReadProtocol.TYPE_ADMIN_CLUSTER_META;
import static com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterMetaReadProtocol.TYPE_NODE_SOURCE;
import static com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterMetaReadProtocol.TYPE_NODE_SOURCE_META;
import static com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterMetaReadProtocol.TYPE_PARTITION_SOURCE;
import static com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterMetaReadProtocol.TYPE_PARTITION_SOURCES;
import static com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterMetaReadProtocol.TYPE_PARTITION_SOURCE_META;
import static com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterMetaReadProtocol.decodeList;
import static com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterMetaReadProtocol.decodeSingle;
import static com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterMetaReadProtocol.encodeRequest;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.CLUSTER_META_READ_TOPIC;

import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 集群元数据统一读门面（leader 权威读）：定向当前 leader 读取 raft 复制的权威副本。
 *
 * <p>元数据全局一套、经 raft 日志复制到各成员；本门面统一读路由为 leader——应答来自 leader 本地 副本（即已提交的最新状态）。允许脏读的调用方应改用 {@code
 * ClusterDispatchService} 的 readLocal* 直读本成员副本（可能落后于 leader）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ClusterMetaReader {
  private final ClusterLeaderFoundService leaderFoundService;
  private final MessagingService messagingService;

  public ClusterMetaReader(
      final ClusterLeaderFoundService leaderFoundService, final MessagingService messagingService) {
    this.leaderFoundService = leaderFoundService;
    this.messagingService = messagingService;
  }

  /** 读节点 source 标识（memberId → sourceId），未分配返回 null */
  public CompletableFuture<NodeSourceRecord> nodeSource(final String memberId) {
    return request(
        encodeRequest(TYPE_NODE_SOURCE, memberId),
        bytes -> decodeSingle(bytes, NodeSourceRecord::new));
  }

  /** 读节点 source 元数据（maxNodeSourceId 计数器） */
  public CompletableFuture<NodeSourceMetaRecord> nodeSourceMeta() {
    return request(
        encodeRequest(TYPE_NODE_SOURCE_META, null),
        bytes -> decodeSingle(bytes, NodeSourceMetaRecord::new));
  }

  /** 读分区 source（partitionGroup:partitionId → sourceId） */
  public CompletableFuture<PartitionSourceRecord> partitionSource(
      final String partitionGroup, final int partitionId) {
    return request(
        encodeRequest(TYPE_PARTITION_SOURCE, partitionGroup + ":" + partitionId),
        bytes -> decodeSingle(bytes, PartitionSourceRecord::new));
  }

  /** 读全部分区 source */
  public CompletableFuture<List<PartitionSourceRecord>> partitionSources() {
    return request(
        encodeRequest(TYPE_PARTITION_SOURCES, null),
        bytes -> decodeList(bytes, PartitionSourceRecord::new));
  }

  /** 读分区 source 元数据 */
  public CompletableFuture<PartitionSourceMetaRecord> partitionSourceMeta() {
    return request(
        encodeRequest(TYPE_PARTITION_SOURCE_META, null),
        bytes -> decodeSingle(bytes, PartitionSourceMetaRecord::new));
  }

  /** 读 admin 面集群拓扑元数据 */
  public CompletableFuture<AdminClusterMetaRecord> adminClusterMeta() {
    return request(
        encodeRequest(TYPE_ADMIN_CLUSTER_META, null),
        bytes -> decodeSingle(bytes, AdminClusterMetaRecord::new));
  }

  private <T> CompletableFuture<T> request(
      final byte[] payload, final Function<byte[], T> decoder) {
    try {
      return messagingService
          .sendAndReceive(leaderFoundService.getLeaderAddress(), CLUSTER_META_READ_TOPIC, payload)
          .thenApply(decoder);
    } catch (final Exception e) {
      final CompletableFuture<T> failed = new CompletableFuture<>();
      failed.completeExceptionally(e);
      return failed;
    }
  }
}
