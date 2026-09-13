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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotType;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.TransferSnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link BootstrapSnapshotStore} 的拍摄复用、删除与启动清理语义：复用保证多请求共享同一镜像 （引用未归零期间不重拍），删除后可重拍，启动清空残留。
 */
final class BootstrapSnapshotStoreTest {

  /** 测试用同步执行器：所有提交在调用线程直接执行，future 立即完成。 */
  private static final class DirectConcurrencyControl implements ConcurrencyControl {

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

  private final AtomicInteger takeCount = new AtomicInteger();

  private TransferSnapshotProvider provider() {
    return new TransferSnapshotProvider() {
      @Override
      public Map<String, Object> takeBootstrapSnapshot(final Path snapshotDirectory) {
        takeCount.incrementAndGet();
        try {
          Files.writeString(snapshotDirectory.resolve("data.txt"), "bootstrap-content");
        } catch (final java.io.IOException e) {
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
    };
  }

  private BootstrapSnapshotStore newStore(final Path partitionDirectory) {
    return new BootstrapSnapshotStore(
        "node-1",
        partitionDirectory,
        provider(),
        new DefaultSnapshotFileInfoProvider(),
        new DirectConcurrencyControl());
  }

  @Test
  void reusesExistingSnapshotWhileNotReleased(@TempDir final Path dir) {
    final var store = newStore(dir);
    final var first = store.takeBootstrapSnapshot(10, 1).join();

    // 第二个请求（水位已推进到 20/2）仍复用第一个镜像：引用未归零期间不重拍
    final var second = store.takeBootstrapSnapshot(20, 2).join();

    assertThat(second.snapshotId()).isEqualTo(first.snapshotId());
    assertThat(takeCount.get()).isEqualTo(1);
  }

  @Test
  void deleteAllowsRetake(@TempDir final Path dir) {
    final var store = newStore(dir);
    final var first = store.takeBootstrapSnapshot(10, 1).join();
    assertThat(store.getBootstrapSnapshot().join()).isNotNull();

    store.deleteBootstrapSnapshot(first.snapshotId()).join();
    assertThat(store.getBootstrapSnapshot().join()).isNull();

    // 删除后下一个请求重新拍摄
    final var second = store.takeBootstrapSnapshot(20, 2).join();
    assertThat(second.snapshotId()).isNotEqualTo(first.snapshotId());
    assertThat(takeCount.get()).isEqualTo(2);
  }

  @Test
  void startClearsResidueFromPreviousRun(@TempDir final Path dir) {
    final var first = newStore(dir);
    final var snapshot = first.takeBootstrapSnapshot(10, 1).join();
    assertThat(Files.exists(snapshot.getPath())).isTrue();

    // 重启：同一目录新建 store，start 应清空残留（引导镜像不跨重启存活）
    final var restarted = newStore(dir);
    restarted.start();

    assertThat(restarted.getBootstrapSnapshot().join()).isNull();
    assertThat(Files.exists(snapshot.getPath())).isFalse();
    // 根目录保留，仍可继续拍摄
    assertThat(Files.isDirectory(dir.resolve("snapshots").resolve(SnapshotType.BOOTSTRAP.directoryName())))
        .isTrue();
    final var retaken = restarted.takeBootstrapSnapshot(30, 3).join();
    assertThat(retaken.getIndex()).isEqualTo(30);
  }

  @Test
  void deleteAllClearsEverything(@TempDir final Path dir) {
    final var store = newStore(dir);
    final var snapshot = store.takeBootstrapSnapshot(10, 1).join();
    store.deleteBootstrapSnapshots().join();

    assertThat(store.getBootstrapSnapshot().join()).isNull();
    assertThat(Files.exists(snapshot.getPath())).isFalse();
  }
}
