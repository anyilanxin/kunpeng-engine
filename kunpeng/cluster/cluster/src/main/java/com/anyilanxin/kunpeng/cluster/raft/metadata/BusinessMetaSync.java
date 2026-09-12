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
import java.time.Duration;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 业务元数据 leader 同步拉取端：follower 检测到日志压缩缺口（appliedIndex+1 &lt; firstIndex）后 向 leader 拉取全量已提交状态（对齐 conf
 * 的 ConfigureRequest 内置通道意图；leader 主动推送需 扩展 raft 线协议，不在本特性范围）。
 */
public final class BusinessMetaSync {
  public static final byte STATUS_AVAILABLE = 1;
  public static final byte STATUS_UNAVAILABLE = 0;

  private static final Logger LOGGER = LoggerFactory.getLogger(BusinessMetaSync.class);
  private static final Function<byte[], byte[]> IDENTITY = Function.identity();

  private final ClusterCommunicationService communicator;
  private final RaftPartitionServer server;
  private final String subject;
  private final Duration requestTimeout;

  public BusinessMetaSync(
      final ClusterCommunicationService communicator,
      final RaftPartitionServer server,
      final String partitionName,
      final Duration requestTimeout) {
    this.communicator = communicator;
    this.server = server;
    this.subject = syncSubjectOf(partitionName);
    this.requestTimeout = requestTimeout;
  }

  /** 同步主题名（与 {@link BusinessMetaServer} 的接收端约定一致）。 */
  public static String syncSubjectOf(final String partitionName) {
    return "business-meta-sync-" + partitionName;
  }

  /**
   * 向 leader 拉取全量业务元数据并应用到本地（仅 follower 由 RaftContext 钩子调用）。 leader 缺失/请求失败/响应不可用 → 重置水位以便下次触发重试；成功
   * → 切 raft 线程应用。
   */
  public void requestSyncFromLeader() {
    final var context = server.getContext();
    final var leader = context.getLeader();
    if (leader == null) {
      context.getBusinessMetaManager().markSyncFailed();
      return;
    }
    communicator
        .send(
            subject,
            new byte[0],
            IDENTITY,
            BusinessMetaSync::decodeSyncResponse,
            leader.memberId(),
            requestTimeout)
        .whenComplete(
            (state, error) -> {
              if (error != null || state == null) {
                LOGGER.warn(
                    "Business meta sync from leader {} failed, will retry on next trigger",
                    leader.memberId(),
                    error);
                server.getContext().getBusinessMetaManager().markSyncFailed();
                return;
              }
              // 切 raft 线程应用，与提交重放/角色切换串行
              server
                  .getContext()
                  .getThreadContext()
                  .execute(
                      () -> server.getContext().getBusinessMetaManager().applySyncedState(state));
            });
  }

  /** 编码同步响应：1 字节状态 + 可选全量状态载荷。 */
  public static byte[] encodeSyncResponse(final boolean available, final byte[] statePayload) {
    if (!available || statePayload == null) {
      return new byte[] {STATUS_UNAVAILABLE};
    }
    final byte[] out = new byte[1 + statePayload.length];
    out[0] = STATUS_AVAILABLE;
    System.arraycopy(statePayload, 0, out, 1, statePayload.length);
    return out;
  }

  /** 解码同步响应：不可用/损坏返回 null（请求端重置水位重试）。 */
  public static byte[] decodeSyncResponse(final byte[] payload) {
    if (payload == null || payload.length < 1 || payload[0] != STATUS_AVAILABLE) {
      return null;
    }
    final byte[] state = new byte[payload.length - 1];
    System.arraycopy(payload, 1, state, 0, state.length);
    return state;
  }
}
