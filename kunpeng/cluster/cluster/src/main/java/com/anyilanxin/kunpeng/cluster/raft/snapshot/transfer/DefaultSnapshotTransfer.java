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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartitionTopology;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.*;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotChunkImpl;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceiveSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceivedSnapshot;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
/**
 * 默认镜像传输服务（Actor）：跨分区拉取与推送。
 *
 * <p>拉取流程（一问一答，一次传输对应一个 transferId）：
 *
 * <pre>
 * ① 拓扑解析目标分区 leader
 * ② 首个 PULL 请求 → 远程返回信息分片（chunkName 承载镜像 id，content 为空）
 *    → 本地 ReceiveSnapshotStore.newReceivedSnapshot 建接收 pending
 * ③ 循环：PULL 请求 → 传输单元 → pending.write(batch) → hasMore 则继续，否则 persist 提交
 * ④ persist 成功后发送 COMPLETE 通知服务端清理读取器
 * </pre>
 *
 * <p>引导拉取流程（跨分区引导新分区，目标成员直连）：首请求为 BOOTSTRAP（请求拍摄引导镜像并 登记引用），完成/放弃通知为
 * RELEASE（释放引用，归零时拍摄端删除引导镜像），其余与常规拉取一致。
 *
 * <p>推送流程（合并转移）：先发信息批，再逐批推送内容，末批写完由目标侧 persist。 任一环节失败：abort pending（拉取时已创建）并以异常完成返回 future。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public class DefaultSnapshotTransfer extends Actor implements SnapshotTransfer {

  /** 镜像传输主题前缀：{@value #SUBJECT_PREFIX}{分区名}。 */
  private static final String SUBJECT_PREFIX = "snapshot-transfer-";

  /** 引导镜像传输主题前缀：{@value #BOOTSTRAP_SUBJECT_PREFIX}{分区名}。 */
  private static final String BOOTSTRAP_SUBJECT_PREFIX = "snapshot-bootstrap-";

  /** 推送/拉取传输单元累计字节上限（缺省 4MiB）。 */
  private static final int DEFAULT_MAX_BATCH_SIZE = 4 * 1024 * 1024;

  private static final Function<byte[], byte[]> IDENTITY = Function.identity();

  private final ClusterMembershipService membershipService;
  private final ClusterCommunicationService communicator;
  // 落盘走接收模块
  private final ReceiveSnapshotStore receiveSnapshotStore;
  private final Duration chunkTimeout;
  private final int maxBatchSize;

  public DefaultSnapshotTransfer(
      final ClusterMembershipService membershipService,
      final ClusterCommunicationService communicator,
      final ReceiveSnapshotStore receiveSnapshotStore,
      final Duration chunkTimeout) {
    this(
        membershipService, communicator, receiveSnapshotStore, chunkTimeout, DEFAULT_MAX_BATCH_SIZE);
  }

  public DefaultSnapshotTransfer(
      final ClusterMembershipService membershipService,
      final ClusterCommunicationService communicator,
      final ReceiveSnapshotStore receiveSnapshotStore,
      final Duration chunkTimeout,
      final int maxBatchSize) {
    this.membershipService = membershipService;
    this.communicator = communicator;
    this.receiveSnapshotStore = receiveSnapshotStore;
    this.chunkTimeout = chunkTimeout;
    this.maxBatchSize = maxBatchSize;
  }

  /** 镜像传输主题：{@value #SUBJECT_PREFIX}{分区名}，服务端按分区注册同名处理器。 */
  public static String subjectOf(final String partitionName) {
    return SUBJECT_PREFIX + partitionName;
  }

  /** 引导镜像传输主题：{@value #BOOTSTRAP_SUBJECT_PREFIX}{分区名}，拍摄端 leader 注册同名处理器。 */
  public static String bootstrapSubjectOf(final String partitionName) {
    return BOOTSTRAP_SUBJECT_PREFIX + partitionName;
  }

  @Override
  public ActorFuture<@Nullable PersistedSnapshot> getLatestSnapshot(final PartitionId partitionId) {
    final CompletableActorFuture<PersistedSnapshot> future = new CompletableActorFuture<>();
    actor.run(
        () -> {
          final var leader = leaderOf(partitionId);
          if (leader.isEmpty()) {
            future.completeExceptionally(
                new SnapshotException(
                    "No known leader for partition " + partitionId + "; cannot pull snapshot"));
            return;
          }
          startPull(
              future,
              leader.get(),
              subjectOf(RaftPartitionTopology.partitionNameOf(partitionId)),
              UUID.randomUUID().toString(),
              RequestCommand.PULL,
              RequestCommand.COMPLETE);
        });
    return future;
  }

  @Override
  public ActorFuture<@Nullable PersistedSnapshot> getBootstrapSnapshot(
      final PartitionId sourcePartitionId, final MemberId sourceMember) {
    final CompletableActorFuture<PersistedSnapshot> future = new CompletableActorFuture<>();
    actor.run(
        () ->
            startPull(
                future,
                sourceMember,
                bootstrapSubjectOf(RaftPartitionTopology.partitionNameOf(sourcePartitionId)),
                UUID.randomUUID().toString(),
                RequestCommand.BOOTSTRAP,
                RequestCommand.RELEASE));
    return future;
  }

  /** 首个请求：取回信息分片，以其 chunkName（镜像 id）创建接收 pending 后进入拉取循环。 */
  private void startPull(
      final CompletableActorFuture<PersistedSnapshot> future,
      final MemberId target,
      final String subject,
      final String transferId,
      final RequestCommand firstCommand,
      final RequestCommand doneCommand) {
    communicator
        .send(
            subject,
            SnapshotTransferCodec.encodeRequest(transferId, firstCommand),
            IDENTITY,
            IDENTITY,
            target,
            chunkTimeout)
        .whenComplete(
            (bytes, error) -> {
              if (error != null) {
                future.completeExceptionally(error);
                return;
              }
              final SnapshotChunk info;
              try {
                info = SnapshotTransferCodec.decodeChunk(bytes);
              } catch (final Exception e) {
                future.completeExceptionally(e);
                return;
              }
              receiveSnapshotStore
                  .newReceivedSnapshot(info.getChunkName())
                  .onComplete(
                      (pending, receiveError) -> {
                        if (receiveError != null) {
                          future.completeExceptionally(receiveError);
                          return;
                        }
                        actor.submit(
                            () ->
                                pullNext(
                                    future, pending, target, subject, transferId, doneCommand));
                      });
            });
  }

  /** 拉取循环：请求下一个传输单元写入 pending；hasMore 则继续，否则提交。 */
  private void pullNext(
      final CompletableActorFuture<PersistedSnapshot> future,
      final ReceivedSnapshot pending,
      final MemberId target,
      final String subject,
      final String transferId,
      final RequestCommand doneCommand) {
    communicator
        .send(
            subject,
            SnapshotTransferCodec.encodeRequest(transferId),
            IDENTITY,
            IDENTITY,
            target,
            chunkTimeout)
        .whenComplete(
            (bytes, error) -> {
              if (error != null) {
                abortAndFail(future, pending, target, subject, transferId, doneCommand, error);
                return;
              }
              final SnapshotChunkBatch batch;
              try {
                batch = SnapshotTransferCodec.decodeChunkBatch(bytes);
              } catch (final Exception e) {
                abortAndFail(future, pending, target, subject, transferId, doneCommand, e);
                return;
              }
              pending
                  .write(batch)
                  .onComplete(
                      (ignored, writeError) -> {
                        if (writeError != null) {
                          abortAndFail(
                              future, pending, target, subject, transferId, doneCommand,
                              writeError);
                          return;
                        }
                        if (batch.hasMore()) {
                          actor.submit(
                              () ->
                                  pullNext(
                                      future, pending, target, subject, transferId, doneCommand));
                        } else {
                          persistPending(
                              future, pending, target, subject, transferId, doneCommand);
                        }
                      });
            });
  }

  /** 提交 pending；成功后发送完成通知（常规传输 COMPLETE 清理读取器，引导传输 RELEASE 释放引用）。 */
  private void persistPending(
      final CompletableActorFuture<PersistedSnapshot> future,
      final ReceivedSnapshot pending,
      final MemberId target,
      final String subject,
      final String transferId,
      final RequestCommand doneCommand) {
    pending
        .persist()
        .onComplete(
            (persisted, error) -> {
              if (error != null) {
                abortAndFail(future, pending, target, subject, transferId, doneCommand, error);
                return;
              }
              communicator.unicast(
                  subject,
                  SnapshotTransferCodec.encodeRequest(transferId, doneCommand),
                  IDENTITY,
                  target,
                  true);
              future.complete(persisted);
            });
  }

  @Override
  public ActorFuture<Void> pushSnapshot(
      final PersistedSnapshot snapshot, final PartitionId targetPartitionId) {
    final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
    actor.run(
        () -> {
          final var leader = leaderOf(targetPartitionId);
          if (leader.isEmpty()) {
            future.completeExceptionally(
                new SnapshotException(
                    "No known leader for partition "
                        + targetPartitionId
                        + "; cannot push snapshot"));
            return;
          }
          pushTo(
              future,
              snapshot,
              SnapshotPushServer.subjectOf(
                  RaftPartitionTopology.partitionNameOf(targetPartitionId)),
              leader.get());
        });
    return future;
  }

  @Override
  public ActorFuture<Void> pushSnapshot(
      final PersistedSnapshot snapshot,
      final PartitionId targetPartitionId,
      final MemberId targetMember) {
    final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
    actor.run(
        () ->
            pushTo(
                future,
                snapshot,
                SnapshotPushServer.subjectOf(
                    RaftPartitionTopology.partitionNameOf(targetPartitionId)),
                targetMember));
    return future;
  }

  private void pushTo(
      final CompletableActorFuture<Void> future,
      final PersistedSnapshot snapshot,
      final String subject,
      final MemberId target) {
    final var batcher = new SnapshotChunkBatcher(snapshot.newChunkReader(UUID.randomUUID()));
    // 先发信息分片：chunkName 承载镜像 id，content 为空
    final var info =
        new SnapshotChunkImpl(snapshot.snapshotId().asString(), 0, 0, 0, ByteBuffer.allocate(0), 0);
    final var infoBatch = new SnapshotChunkBatch(TransferKind.FILE_CHUNKS, List.of(info), true);
    communicator
        .send(
            subject,
            SnapshotTransferCodec.encodeChunkBatch(infoBatch),
            IDENTITY,
            IDENTITY,
            target,
            chunkTimeout)
        .whenComplete(
            (bytes, error) -> {
              if (error != null) {
                batcher.close();
                future.completeExceptionally(error);
                return;
              }
              pushNext(future, batcher, target, subject);
            });
  }

  private void pushNext(
      final CompletableActorFuture<Void> future,
      final SnapshotChunkBatcher batcher,
      final MemberId target,
      final String subject) {
    final SnapshotChunkBatch batch = batcher.nextBatch(maxBatchSize);
    communicator
        .send(
            subject,
            SnapshotTransferCodec.encodeChunkBatch(batch),
            IDENTITY,
            IDENTITY,
            target,
            chunkTimeout)
        .whenComplete(
            (bytes, error) -> {
              if (error != null) {
                batcher.close();
                future.completeExceptionally(error);
                return;
              }
              if (batch.hasMore()) {
                actor.submit(() -> pushNext(future, batcher, target, subject));
              } else {
                batcher.close();
                future.complete(null);
              }
            });
  }

  private Optional<MemberId> leaderOf(final PartitionId partitionId) {
    final var topology = new RaftPartitionTopology(membershipService);
    return topology.leaderOf(partitionId).map(member -> member.id());
  }

  /**
   * 放弃本次传输：abort pending 并以异常完成 future；引导传输（doneCommand 为 RELEASE）时尽力 通知拍摄端释放引用，失败仅记录——引用泄漏由拍摄端重启清理兜底。
   */
  private void abortAndFail(
      final CompletableActorFuture<PersistedSnapshot> future,
      final ReceivedSnapshot pending,
      final MemberId target,
      final String subject,
      final String transferId,
      final RequestCommand doneCommand,
      final Throwable error) {
    pending.abort();
    if (doneCommand == RequestCommand.RELEASE) {
      communicator.unicast(
          subject,
          SnapshotTransferCodec.encodeRequest(transferId, doneCommand),
          IDENTITY,
          target,
          true);
    }
    future.completeExceptionally(error);
  }
}
