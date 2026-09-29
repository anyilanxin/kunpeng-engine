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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.RequestCommand;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotTransferCodec;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.TransferSnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSnapshotFileInfoProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link BootstrapSnapshotServer} 的 transferId 引用计数语义：多个引导请求共享同一镜像， 引用未归零时 RELEASE 只减少请求方，最后一个 RELEASE
 * 才真正删除镜像。
 */
@ExtendWith(MockitoExtension.class)
final class BootstrapSnapshotServerTest {

  private static final String PARTITION_NAME = "core-group-partition-1";

  @Mock private ClusterCommunicationService communicator;

  /** replyTo 注册的请求处理器（IDENTITY 编解码，直接喂编码后的 payload）。 */
  private final AtomicReference<Function<byte[], CompletableFuture<byte[]>>> handler =
      new AtomicReference<>();

  private final AtomicLong commitIndex = new AtomicLong(10);
  private final AtomicLong term = new AtomicLong(1);

  private BootstrapSnapshotStore store;
  private BootstrapSnapshotServer server;

  @BeforeEach
  void setUp(@TempDir final Path dir) {
    store =
        new BootstrapSnapshotStore(
            "node-1",
            dir,
            new TransferSnapshotProvider() {
              @Override
              public Map<String, Object> takeBootstrapSnapshot(final Path snapshotDirectory) {
                try {
                  Files.writeString(snapshotDirectory.resolve("data.txt"), "bootstrap-content");
                } catch (final IOException e) {
                  throw new java.io.UncheckedIOException(e);
                }
                return Map.of();
              }

              @Override
              public Map<String, Object> takeMergeSnapshot(final Path snapshotDirectory) {
                throw new UnsupportedOperationException("Not used in bootstrap tests");
              }

              @Override
              public void close() {
                // 测试无需清理
              }
            },
            new DefaultSnapshotFileInfoProvider(),
            new DirectConcurrencyControl());
    store.start();
    server =
        new BootstrapSnapshotServer(
            communicator, PARTITION_NAME, store, commitIndex::get, term::get);
  }

  @Test
  void registersAndUnregistersBootstrapSubject() {
    captureHandler();

    verify(communicator)
        .replyTo(eq(BootstrapSnapshotServer.subjectOf(PARTITION_NAME)), any(), any(), any());

    server.unregister();
    verify(communicator).unsubscribe(BootstrapSnapshotServer.subjectOf(PARTITION_NAME));
  }

  @Test
  void bootstrapTakesSnapshotOnceAndIsReusedByFurtherPullers() {
    captureHandler();

    final SnapshotChunk first = serveBootstrap("t1");
    final SnapshotChunk second = serveBootstrap("t2");

    // 第二个请求复用同一镜像（同一时段多个新分区引导内容一致）
    assertThat(second.getChunkName()).isEqualTo(first.getChunkName());
    assertThat(store.getBootstrapSnapshot().join()).isNotNull();
  }

  @Test
  void releaseDeletesSnapshotOnlyWhenLastReferenceRemains() {
    captureHandler();
    final SnapshotChunk info = serveBootstrap("t1");
    serveBootstrap("t2");

    // 第一个引用释放：还有 t2 在拉取，镜像保留
    serve(RequestCommand.RELEASE, "t1");
    assertThat(store.getBootstrapSnapshot().join()).isNotNull();

    // 最后一个引用释放：镜像真正删除
    serve(RequestCommand.RELEASE, "t2");
    assertThat(store.getBootstrapSnapshot().join()).isNull();

    // 重复 RELEASE 幂等
    serve(RequestCommand.RELEASE, "t2");
    assertThat(store.getBootstrapSnapshot().join()).isNull();
  }

  @Test
  void pullWithoutSessionFailsAndBootstrapRetriesResendInfo() {
    captureHandler();

    // 未知 transferId 的 PULL：会话丢失（如拍摄端重启），需以新 transferId 重来
    assertThatThrownBy(() -> serve(RequestCommand.PULL, "unknown"))
        .isInstanceOf(java.util.concurrent.CompletionException.class);

    final SnapshotChunk info = serveBootstrap("t1");
    // 同 transferId 重试 BOOTSTRAP：直接重发信息分片，不重拍
    final SnapshotChunk retry = serveBootstrap("t1");
    assertThat(retry.getChunkName()).isEqualTo(info.getChunkName());
  }

  @Test
  void bootstrapRejectsWhenNoCommittedData() {
    captureHandler();
    commitIndex.set(0);

    assertThatThrownBy(() -> serve(RequestCommand.BOOTSTRAP, "t1"))
        .isInstanceOf(java.util.concurrent.CompletionException.class);
    assertThat(store.getBootstrapSnapshot().join()).isNull();
  }

  /** 注册处理器并捕获 serve 函数，后续直接调用以模拟远程请求。 */
  private void captureHandler() {
    doAnswer(
            invocation -> {
              handler.set(invocation.getArgument(2));
              return null;
            })
        .when(communicator)
        .replyTo(any(), any(), any(), any());
    server.register();
  }

  private SnapshotChunk serveBootstrap(final String transferId) {
    final byte[] payload = serve(RequestCommand.BOOTSTRAP, transferId);
    return SnapshotTransferCodec.decodeChunk(payload);
  }

  private byte[] serve(final RequestCommand command, final String transferId) {
    return handler.get()
        .apply(SnapshotTransferCodec.encodeRequest(transferId, command))
        .join();
  }

  /** 测试用同步执行器（与 BootstrapSnapshotStoreTest 相同语义）。 */
  private static final class DirectConcurrencyControl
      implements com.anyilanxin.kunpeng.scheduler.ConcurrencyControl {

    @Override
    public <T> void runOnCompletion(
        final com.anyilanxin.kunpeng.scheduler.future.ActorFuture<T> future,
        final java.util.function.BiConsumer<T, Throwable> callback) {
      future.onComplete(callback, Runnable::run);
    }

    @Override
    public <T> void runOnCompletion(
        final java.util.Collection<com.anyilanxin.kunpeng.scheduler.future.ActorFuture<T>> futures,
        final java.util.function.Consumer<Throwable> callback) {
      futures.forEach(
          future -> future.onComplete((v, error) -> callback.accept(error), Runnable::run));
    }

    @Override
    public com.anyilanxin.kunpeng.scheduler.future.ActorFuture<Void> run(
        final Runnable action) {
      try {
        action.run();
        return com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture.completed(null);
      } catch (final Exception e) {
        final var future =
            new com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture<Void>();
        future.completeExceptionally(e);
        return future;
      }
    }

    @Override
    public <T> com.anyilanxin.kunpeng.scheduler.future.ActorFuture<T> call(
        final Callable<T> callable) {
      final var future =
          new com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture<T>();
      try {
        future.complete(callable.call());
      } catch (final Exception e) {
        future.completeExceptionally(e);
      }
      return future;
    }

    @Override
    public com.anyilanxin.kunpeng.scheduler.ScheduledTimer schedule(
        final java.time.Duration delay, final Runnable runnable) {
      throw new UnsupportedOperationException("Not used in these tests");
    }
  }
}
