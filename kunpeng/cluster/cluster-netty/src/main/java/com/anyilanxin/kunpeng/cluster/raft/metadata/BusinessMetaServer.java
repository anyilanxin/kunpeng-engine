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
package com.anyilanxin.kunpeng.cluster.raft.metadata;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.RaftPartitionServer;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.BusinessMetaStore;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 业务元数据修改请求接收端（按分区订阅 {@code business-meta-}{分区名} 主题，全部角色常驻注册）。
 *
 * <p>修改请求处理规则：本机 leader → 追加日志；非 leader 且已知 leader → 转发（防环：转发请求 不再转发）；无 leader → NO_LEADER 拒绝。另常驻注册
 * leader 同步拉取主题 （{@link BusinessMetaSync#syncSubjectOf}）。
 */
public final class BusinessMetaServer {

  private static final Function<byte[], byte[]> IDENTITY = Function.identity();

  private final ClusterCommunicationService communicator;
  private final RaftPartitionServer server;
  private final String subject;
  private final String syncSubject;

  public BusinessMetaServer(
      final ClusterCommunicationService communicator,
      final RaftPartitionServer server,
      final String partitionName) {
    this.communicator = communicator;
    this.server = server;
    this.subject = subjectOf(partitionName);
    this.syncSubject = BusinessMetaSync.syncSubjectOf(partitionName);
  }

  public static String subjectOf(final String partitionName) {
    return "business-meta-" + partitionName;
  }

  /** 注册处理器（分区 server 创建时调用）：修改 + 同步两个主题，全部角色常驻注册。 */
  public void register() {
    communicator.replyTo(subject, IDENTITY, this::serve, IDENTITY);
    communicator.replyTo(syncSubject, IDENTITY, this::serveSync, IDENTITY);
  }

  /** 注销处理器（分区停止时调用）。 */
  public void unregister() {
    communicator.unsubscribe(subject);
    communicator.unsubscribe(syncSubject);
  }

  private CompletableFuture<byte[]> serve(final byte[] payload) {
    final BusinessMetaTransfer.Request request = BusinessMetaTransfer.decodeRequest(payload);
    return handle(request).thenApply(BusinessMetaTransfer::encodeResponse);
  }

  /** 同步拉取接收端：leader 返回全量状态；非 leader（易主瞬间收到旧请求）显式不可用。 */
  private CompletableFuture<byte[]> serveSync(final byte[] payload) {
    final var context = server.getContext();
    if (!context.isLeader()) {
      return CompletableFuture.completedFuture(BusinessMetaSync.encodeSyncResponse(false, null));
    }
    final PartitionBusinessMeta.AppliedState applied =
        context.getBusinessMetaManager().current().appliedState();
    return CompletableFuture.completedFuture(
        BusinessMetaSync.encodeSyncResponse(
            true, BusinessMetaStore.encode(applied.index(), applied.term(), applied.entries())));
  }

  CompletableFuture<BusinessMetaUpdateResponse> handle(final BusinessMetaTransfer.Request request) {
    if (server.getContext().isLeader()) {
      return server.appendBusinessMeta(request.entries());
    }
    final var leader = server.getContext().getLeader();
    if (leader == null) {
      return CompletableFuture.completedFuture(BusinessMetaUpdateResponse.noLeader());
    }
    if (request.forwarded()) {
      // 转发到 leader 后仍被转发，说明 leader 已失联/易主：按无主拒绝，由发起端重试
      return CompletableFuture.completedFuture(
          BusinessMetaUpdateResponse.error(
              "NO_LEADER: forwarded request cannot be forwarded again"));
    }
    return server.forwardBusinessMetaTo(leader.memberId(), request.entries());
  }
}
