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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.ArchivedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.BlockStreamReader;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.IncomingReplica;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotBlock;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotVault;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 引导快照传输服务：处理跨节点引导快照的请求与传输。
 *
 * <p><b>源分区（leader 节点）</b>调用 {@link #startServing()} 监听引导请求：
 * 收到 init → 若无引导快照则创建 → 返回快照 ID → 后续 chunk 请求逐块返回 {@code SnapshotBlock}。
 *
 * <p><b>目标分区</b>调用 {@link #pullBootstrapSnapshot(MemberId)}：
 * 发起 init → 收到快照 ID → 逐块拉取并写入本地 vault → 全部完成后落档返回
 * {@code ArchivedSnapshot} → 上层触发 {@code handler.onRecoverFromSnapshot}。
 */
public final class BootstrapTransferService {

  private static final Logger LOG = LoggerFactory.getLogger(BootstrapTransferService.class);
  private static final Duration TRANSFER_TIMEOUT = Duration.ofMinutes(10);
  private static final int MAX_BLOCK_BYTES = 1024 * 1024;
  private static final String END_MARKER = "__end__";

  private final ClusterCommunicationService communicationService;
  private final SnapshotVault vault;
  private final SnapshotHandler snapshotHandler;
  private final String subject;
  private volatile BlockStreamReader currentReader;

  public BootstrapTransferService(
      final ClusterCommunicationService communicationService,
      final SnapshotVault vault,
      final SnapshotHandler snapshotHandler,
      final PartitionId partitionId) {
    this.communicationService = communicationService;
    this.vault = vault;
    this.snapshotHandler = snapshotHandler;
    this.subject = "raft-bootstrap-" + partitionId.group() + "-" + partitionId.id();
  }

  // ===== 源分区（leader）侧 =====

  /** 启动引导快照服务（在源分区的 leader 节点调用） */
  public CompletableFuture<Void> startServing() {
    LOG.info("启动引导快照服务（subject={}）", subject);
    return communicationService.subscribe(
        subject,
        bytes -> bytes,
        this::handleRequest,
        bytes -> bytes);
  }

  /** 停止服务 */
  public void stopServing() {
    communicationService.unsubscribe(subject);
    currentReader = null;
  }

  private CompletableFuture<byte[]> handleRequest(final byte[] request) {
    final var type = decodeType(request);
    return switch (type) {
      case "init" -> handleInit();
      case "chunk" -> handleChunk();
      default -> CompletableFuture.completedFuture(encodeError("未知请求类型: " + type));
    };
  }

  /** init 请求：若无引导快照则创建，返回快照 ID */
  private CompletableFuture<byte[]> handleInit() {
    return CompletableFuture.supplyAsync(() -> {
      var bootstrap = vault.getBootstrapSnapshot();
      if (bootstrap.isEmpty() && vault.getLatestSnapshot().isPresent()) {
        // 复制最新快照到 bootstrap 区
        final var latest = vault.getLatestSnapshot().get();
        vault.copyForBootstrap(latest.ref().toString()).join();
        bootstrap = vault.getBootstrapSnapshot();
      }
      if (bootstrap.isEmpty()) {
        return encodeError("源分区无可用快照");
      }
      final var archived = bootstrap.get();
      currentReader = archived.blockReader(MAX_BLOCK_BYTES);
      LOG.info("提供引导快照 {}（blocks={}）", archived.ref(), currentReader.totalBlocks());
      return encodeSnapshotId(archived.ref().toString());
    });
  }

  /** chunk 请求：返回下一个 SnapshotBlock；传完返回结束标记 */
  private CompletableFuture<byte[]> handleChunk() {
    return CompletableFuture.supplyAsync(() -> {
      final var reader = currentReader;
      if (reader == null || !reader.hasNext()) {
        currentReader = null;
        return END_MARKER.getBytes(StandardCharsets.UTF_8);
      }
      return reader.next().encode();
    });
  }

  // ===== 目标分区侧 =====

  /**
   * 从源分区拉取引导快照：init → 逐块拉取 → 本地 vault 接收落档。
   *
   * @param sourceNode 源分区所在节点
   * @return 接收落档后的快照；不可用返回 null
   */
  public CompletableFuture<ArchivedSnapshot> pullBootstrapSnapshot(final MemberId sourceNode) {
    LOG.info("从 {} 拉取引导快照（subject={}）", sourceNode, subject);
    return communicationService
        .send(subject, encodeType("init"), this::identity, this::identity,
            sourceNode, TRANSFER_TIMEOUT)
        .thenCompose(response -> {
          if (response == null || response.length == 0) {
            LOG.warn("源分区 {} 无引导快照", sourceNode);
            return CompletableFuture.<ArchivedSnapshot>completedFuture(null);
          }
          final var snapshotId = decodeSnapshotId(response);
          if (snapshotId == null) {
            LOG.warn("源分区 {} 返回错误: {}", sourceNode, decodeErrorMessage(response));
            return CompletableFuture.<ArchivedSnapshot>completedFuture(null);
          }
          LOG.info("引导快照 ID: {}，开始逐块拉取", snapshotId);
          return vault.receive(snapshotId)
              .thenCompose(replica -> pullBlocks(sourceNode, replica));
        });
  }

  /** 逐块拉取并应用到接收副本，全部完成后提交落档 */
  private CompletableFuture<ArchivedSnapshot> pullBlocks(
      final MemberId sourceNode, final IncomingReplica replica) {
    return communicationService
        .send(subject, encodeType("chunk"), this::identity, this::identity,
            sourceNode, TRANSFER_TIMEOUT)
        .thenCompose(response -> {
          if (response == null || isEndMarker(response)) {
            LOG.info("全部块已接收，提交落档");
            return vault.commitReplica(replica).thenApply(v ->
                vault.getLatestSnapshot().orElse(null));
          }
          try {
            replica.apply(SnapshotBlock.decode(response));
          } catch (final Exception e) {
            LOG.error("块应用失败", e);
            replica.abort();
            return CompletableFuture.<ArchivedSnapshot>completedFuture(null);
          }
          return pullBlocks(sourceNode, replica);
        });
  }

  // ===== 编码/解码 =====

  private static byte[] encodeType(final String type) {
    try {
      final var bos = new ByteArrayOutputStream();
      new DataOutputStream(bos).writeUTF(type);
      return bos.toByteArray();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String decodeType(final byte[] bytes) {
    try {
      return new DataInputStream(new ByteArrayInputStream(bytes)).readUTF();
    } catch (final IOException e) {
      return "";
    }
  }

  private static byte[] encodeSnapshotId(final String snapshotId) {
    try {
      final var bos = new ByteArrayOutputStream();
      final var out = new DataOutputStream(bos);
      out.writeBoolean(true);
      out.writeUTF(snapshotId);
      out.flush();
      return bos.toByteArray();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] encodeError(final String message) {
    try {
      final var bos = new ByteArrayOutputStream();
      final var out = new DataOutputStream(bos);
      out.writeBoolean(false);
      out.writeUTF(message);
      out.flush();
      return bos.toByteArray();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String decodeSnapshotId(final byte[] response) {
    try {
      final var in = new DataInputStream(new ByteArrayInputStream(response));
      return in.readBoolean() ? in.readUTF() : null;
    } catch (final IOException e) {
      return null;
    }
  }

  private static String decodeErrorMessage(final byte[] response) {
    try {
      final var in = new DataInputStream(new ByteArrayInputStream(response));
      return in.readBoolean() ? null : in.readUTF();
    } catch (final IOException e) {
      return "解码失败";
    }
  }

  private static boolean isEndMarker(final byte[] response) {
    return END_MARKER.equals(new String(response, StandardCharsets.UTF_8));
  }

  private byte[] identity(final byte[] bytes) {
    return bytes;
  }
}
