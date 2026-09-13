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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.TestSnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkBatch;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotTransferCodec;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultRaftSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSimpleFileVerificationStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotChunkImpl;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link SnapshotPushServer}：info 批识别、合并镜像接收落地（merge 目录）后触发合并流， 以及合并完成等待请求按镜像 id 应答。
 */
@ExtendWith(MockitoExtension.class)
final class SnapshotPushServerTest {

  private static final String PARTITION_NAME = "core-group-partition-1";

  @Mock private ClusterCommunicationService communicator;

  /** 按主题捕获 replyTo 注册的处理器。 */
  private final Map<String, Function<byte[], CompletableFuture<byte[]>>> handlers =
      new ConcurrentHashMap<>();

  /** 源端镜像（推送内容来源）。 */
  private PersistedSnapshot sourceSnapshot;
  /** 目标端 merge 目录存储。 */
  private DefaultRaftSnapshotStore mergeStore;
  /** 合并流回调捕获：返回的 future 由测试控制完成时机。 */
  private final AtomicReference<CompletableFuture<Void>> mergeFlowFuture = new AtomicReference<>();
  private PersistedSnapshot mergeReceived;

  @BeforeEach
  void setUp(@TempDir final Path dir) throws Exception {
    // 源端：真实拍摄一个镜像并用其读取器产生推送分片
    final var sourceStore =
        new DefaultRaftSnapshotStore(
            "node-1",
            dir.resolve("source"),
            1,
            new DefaultSimpleFileVerificationStore(),
            new DefaultSnapshotFileInfoProvider(),
            new TestSnapshotProvider(
                snapshotDir -> {
                  Files.writeString(snapshotDir.resolve("data.txt"), "merge-content");
                  return Map.of("key", "value");
                }),
            new SynchronousConcurrencyControl());
    sourceStore.start();
    sourceSnapshot = sourceStore.newTransientSnapshot(10, 1).join().persist().join();

    // 目标端：merge 目录的接收存储
    mergeStore =
        new DefaultRaftSnapshotStore(
            "node-1",
            dir.resolve("merge"),
            1,
            new DefaultSimpleFileVerificationStore(),
            new DefaultSnapshotFileInfoProvider(),
            new TestSnapshotProvider(
                snapshotDir -> {
                  throw new UnsupportedOperationException("Target merge store only receives");
                }),
            new SynchronousConcurrencyControl());
    mergeStore.start();

    final var server =
        new SnapshotPushServer(
            communicator,
            PARTITION_NAME,
            mergeStore,
            received -> {
              mergeReceived = received;
              final var future = new CompletableFuture<Void>();
              mergeFlowFuture.set(future);
              return future;
            });

    doAnswer(
            invocation -> {
              handlers.put(
                  invocation.getArgument(0),
                  invocation.getArgument(2));
              return null;
            })
        .when(communicator)
        .replyTo(any(), any(), any(), any());
    server.register();
  }

  @Test
  void shouldDetectInfoBatchOnlyForValidSnapshotIdChunkName() {
    // 内容分片名固定带 '@' 偏移（见 FileSnapshotChunkReader），不会误判为 info
    assertThat(SnapshotPushServer.isInfoChunkName("6c6561646572-100-5")).isTrue();
    assertThat(SnapshotPushServer.isInfoChunkName("data.bin@0")).isFalse();
    // 即使 '@' 前的部分恰好是合法镜像 id，含 '@' 即为内容分片名，不能当 info 批
    assertThat(SnapshotPushServer.isInfoChunkName("6c6561646572-100-5@0")).isFalse();
    assertThat(SnapshotPushServer.isInfoChunkName("not-a-valid-id")).isFalse();
    assertThat(SnapshotPushServer.isInfoChunkName(null)).isFalse();
  }

  @Test
  void registersPushAndAwaitSubjects() {
    verify(communicator)
        .replyTo(eq(SnapshotPushServer.subjectOf(PARTITION_NAME)), any(), any(), any());
    verify(communicator)
        .replyTo(eq(SnapshotPushServer.awaitSubjectOf(PARTITION_NAME)), any(), any(), any());
  }

  @Test
  void receivesMergeSnapshotIntoMergeStoreAndTriggersMergeFlow() throws Exception {
    pushAllBatches();

    // 接收完成：镜像落地 merge 目录且 id 一致，合并流被触发
    assertThat(mergeFlowFuture.get()).isNotNull();
    assertThat(mergeReceived).isNotNull();
    assertThat(mergeReceived.snapshotId()).isEqualTo(sourceSnapshot.snapshotId());
    assertThat(Files.readString(mergeReceived.getPath().resolve("data.txt")))
        .isEqualTo("merge-content");
    assertThat(mergeStore.getLatestSnapshot()).isPresent();
    assertThat(mergeStore.getLatestSnapshot().get().snapshotId())
        .isEqualTo(sourceSnapshot.snapshotId());
  }

  @Test
  void awaitRepliesAfterMergeFlowCompletesAndFailsForUnknownSnapshot() {
    pushAllBatches();

    // 合并流未完成：等待请求挂起
    final var awaitFuture = serveAwait(sourceSnapshot.snapshotId().asString());
    assertThat(awaitFuture).isNotDone();

    // 合并流完成：等待请求应答（空 payload）
    mergeFlowFuture.get().complete(null);
    assertThat(awaitFuture).isCompletedWithValueMatching(bytes -> bytes.length == 0);

    // 未知镜像 id（重启清理后）：立即失败，源分区整体重推
    final var unknownAwait = serveAwait("6e6f64652d39-999-1");
    assertThat(unknownAwait).isCompletedExceptionally();
    assertThat(unknownAwait).failsWithin(java.time.Duration.ofMillis(0))
        .withThrowableThat()
        .havingCause()
        .isInstanceOf(SnapshotException.class);
  }

  /** 按生产推送协议喂数据：info 批 + 装批器产生的全部内容批。 */
  private void pushAllBatches() {
    final var pushHandler = handlers.get(SnapshotPushServer.subjectOf(PARTITION_NAME));
    assertThat(pushHandler).isNotNull();

    // info 批：chunkName 承载镜像 id、content 为空
    final var info =
        new SnapshotChunkImpl(
            sourceSnapshot.snapshotId().asString(), 0, 0, 0, ByteBuffer.allocate(0), 0);
    pushHandler
        .apply(
            SnapshotTransferCodec.encodeChunkBatch(
                new SnapshotChunkBatch(
                    com.anyilanxin.kunpeng.cluster.raft.snapshot.TransferKind.FILE_CHUNKS,
                    List.of(info),
                    true)))
        .join();

    // 内容批：源镜像读取器逐批编码推送
    final var batcher =
        new SnapshotChunkBatcher(sourceSnapshot.newChunkReader(UUID.randomUUID()));
    SnapshotChunkBatch batch = batcher.nextBatch(4 * 1024 * 1024);
    while (true) {
      pushHandler.apply(SnapshotTransferCodec.encodeChunkBatch(batch)).join();
      if (!batch.hasMore()) {
        break;
      }
      batch = batcher.nextBatch(4 * 1024 * 1024);
    }
    batcher.close();
  }

  private CompletableFuture<byte[]> serveAwait(final String snapshotId) {
    final var awaitHandler = handlers.get(SnapshotPushServer.awaitSubjectOf(PARTITION_NAME));
    assertThat(awaitHandler).isNotNull();
    return awaitHandler.apply(snapshotId.getBytes(StandardCharsets.UTF_8));
  }

  /** 测试用同步执行器：所有提交在调用线程直接执行，future 立即完成。 */
  private static final class SynchronousConcurrencyControl implements ConcurrencyControl {

    @Override
    public <T> void runOnCompletion(
        final ActorFuture<T> future, final BiConsumer<T, Throwable> callback) {
      future.onComplete(callback, Runnable::run);
    }

    @Override
    public <T> void runOnCompletion(
        final Collection<ActorFuture<T>> futures, final Consumer<Throwable> callback) {
      futures.forEach(
          future -> future.onComplete((v, error) -> callback.accept(error), Runnable::run));
    }

    @Override
    public ActorFuture<Void> run(final Runnable action) {
      try {
        action.run();
        return CompletableActorFuture.completed(null);
      } catch (final Exception e) {
        final var future = new CompletableActorFuture<Void>();
        future.completeExceptionally(e);
        return future;
      }
    }

    @Override
    public <T> ActorFuture<T> call(final Callable<T> callable) {
      final var future = new CompletableActorFuture<T>();
      try {
        future.complete(callable.call());
      } catch (final Exception e) {
        future.completeExceptionally(e);
      }
      return future;
    }

    @Override
    public com.anyilanxin.kunpeng.scheduler.ScheduledTimer schedule(
        final Duration delay, final Runnable runnable) {
      throw new UnsupportedOperationException("Not used in these tests");
    }
  }
}
