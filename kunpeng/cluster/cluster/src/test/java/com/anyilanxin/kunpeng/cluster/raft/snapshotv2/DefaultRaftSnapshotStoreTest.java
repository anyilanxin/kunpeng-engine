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
package com.anyilanxin.kunpeng.cluster.raft.snapshotv2;

import com.anyilanxin.kunpeng.cluster.raft.TestSnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.*;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultRaftSnapshotStore;
import com.anyilanxin.kunpeng.kvstore.snapshot.SnapshotFileInfo;
import com.anyilanxin.kunpeng.kvstore.snapshot.SnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSimpleFileVerificationStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotChunkImpl;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class DefaultRaftSnapshotStoreTest {

  @TempDir
  Path tmpDir;
  private final ConcurrencyControl actor = new SynchronousConcurrencyControl();

  @Test
  void shouldTakeAndPersistSnapshot() {
    final var store = newStore("node-1", 1);
    store.start();

    final var pending = store.newTransientSnapshot(5, 1).join();
    pending.persist().join();

    final var latest = store.getLatestSnapshot();
    assertThat(latest).isPresent();
    assertThat(latest.get().getIndex()).isEqualTo(5);
    assertThat(latest.get().getTerm()).isEqualTo(1);
    assertThat(Files.exists(latest.get().getPath().resolve("data"))).isTrue();
    assertThat(Files.exists(latest.get().getChecksumPath())).isTrue();
  }

  /** 拍摄未写入任何内容时生成仅含元数据文件的空镜像：拍摄正常完成、可提交、重启后仍可加载。 */
  @Test
  void shouldPersistMetadataOnlySnapshotWhenTakenContentIsEmpty() {
    final var store = newStore("node-1", 1, dir -> Map.of());
    store.start();

    final var pending = store.newTransientSnapshot(5, 1).join();
    pending.persist().join();

    final var latest = store.getLatestSnapshot();
    assertThat(latest).isPresent();
    assertThat(latest.get().getIndex()).isEqualTo(5);
    try (final var entries = Files.list(latest.get().getPath())) {
      assertThat(entries.map(entry -> entry.getFileName().toString()).toList())
          .containsExactly("snapshot.metadata");
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    // 重启后仍可加载：清单仅元数据，校验不触发分区 provider 重算
    final var reloaded = newStore("node-1", 1, dir -> Map.of());
    reloaded.start();
    assertThat(reloaded.getLatestSnapshot()).isPresent();
    assertThat(reloaded.getLatestSnapshot().get().getIndex()).isEqualTo(5);
  }

  @Test
  void shouldReloadSnapshotOnStart() {
    final var store = newStore("node-1", 1);
    store.start();
    store.newTransientSnapshot(7, 2).join().persist().join();

    // 重新构建同目录 store 并 start，应加载出已持久化的镜像
    final var reloaded = newStore("node-1", 1);
    reloaded.start();

    final var latest = reloaded.getLatestSnapshot();
    assertThat(latest).isPresent();
    assertThat(latest.get().getIndex()).isEqualTo(7);
  }

  @Test
  void shouldEnforceRetention() {
    final var store = newStore("node-1", 2);
    store.start();
    store.newTransientSnapshot(1, 1).join().persist().join();
    store.newTransientSnapshot(2, 1).join().persist().join();
    store.newTransientSnapshot(3, 1).join().persist().join();

    assertThat(store.getCurrentSnapshotIndex()).isEqualTo(3);
    final var root = store.getPath();
    try (final var entries = Files.list(root)) {
      final var snapshotDirs = entries.filter(Files::isDirectory).count();
      assertThat(snapshotDirs).isEqualTo(2);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** 保留策略作用于存储内全部 raft 相关镜像（拍摄 + 接收）；bootstrap/merge 镜像在各自类型目录的独立 store，不经过本策略。 */
  @Test
  void shouldPruneReceivedSnapshotByRetention() {
    final var store = newStore("node-1", 1);
    store.start();

    // 先接收一个外部节点拍摄的镜像，随后本节点连续拍摄触发保留策略（保留 1 个）
    final var receivedId = new SnapshotId("node-0", 1, 1);
    final var pending = store.newReceivedSnapshot(receivedId.asString()).join();
    final byte[] metadataBytes = "version=1\nkey=value\n".getBytes(StandardCharsets.UTF_8);
    pending
        .write(
            new SnapshotChunkBatch(
                TransferKind.FILE_CHUNKS,
                List.of(chunkOf(SnapshotMetadata.METADATA_FILE_NAME + "@0", metadataBytes, 0)),
                false))
        .join();
    pending.persist().join();

    store.newTransientSnapshot(5, 1).join().persist().join();
    store.newTransientSnapshot(6, 1).join().persist().join();

    // 只保留最新 1 个：旧拍摄镜像与接收镜像都被删除
    assertThat(Files.exists(store.getPath().resolve(receivedId.asString()))).isFalse();
    assertThat(Files.exists(store.getPath().resolve(new SnapshotId("node-1", 5, 1).asString())))
        .isFalse();
    assertThat(Files.exists(store.getPath().resolve(new SnapshotId("node-1", 6, 1).asString())))
        .isTrue();
  }

  /** 启动对无效残留目录（如提交中断缺 .sfc）只跳过不删除；同 id 重新提交时覆盖残留完成恢复。 */
  @Test
  void shouldKeepResidueOnStartAndAllowRecommitOfSameId() {
    // 模拟提交中断残留：目录存在但缺 .sfc 标记
    final var residueId = new SnapshotId("node-1", 1, 1);
    final var residueDir = tmpDir.resolve(residueId.asString());
    writeDataFile(residueDir, "data");

    final var store = newStore("node-1", 1);
    store.start();

    // 启动只加载不清理：残留目录保留在磁盘，且不作为有效镜像加载
    assertThat(Files.exists(residueDir)).isTrue();
    assertThat(store.getLatestSnapshot()).isEmpty();

    // 同 id 重新提交：覆盖残留完成恢复
    final var pending = store.newReceivedSnapshot(residueId.asString()).join();
    final byte[] metadataBytes = "version=1\nkey=value\n".getBytes(StandardCharsets.UTF_8);
    pending
        .write(
            new SnapshotChunkBatch(
                TransferKind.FILE_CHUNKS,
                List.of(chunkOf(SnapshotMetadata.METADATA_FILE_NAME + "@0", metadataBytes, 0)),
                false))
        .join();
    pending.persist().join();

    assertThat(store.getLatestSnapshot()).isPresent();
    assertThat(store.getLatestSnapshot().get().getIndex()).isEqualTo(1);
  }

  @Test
  void shouldReceiveAndPersistSnapshot() {
    final var store = newStore("node-1", 1);
    store.start();

    final var snapshotId = new SnapshotId("node-1", 9, 1);
    final var pending = store.newReceivedSnapshot(snapshotId.asString()).join();

    final byte[] metadataBytes = "version=1\nkey=value\n".getBytes(StandardCharsets.UTF_8);
    final SnapshotChunk metadataChunk =
      chunkOf(SnapshotMetadata.METADATA_FILE_NAME + "@0", metadataBytes, 0);
    pending.write(new SnapshotChunkBatch(TransferKind.FILE_CHUNKS, List.of(metadataChunk), false)).join();
    pending.persist().join();

    assertThat(store.getCurrentSnapshotIndex()).isEqualTo(9);
    assertThat(store.getLatestSnapshot().get().getMetadata().metaInfo()).containsEntry("key", "value");
  }

  @Test
  void shouldReportCompactionBound() {
    final var store = newStore("node-1", 3);
    store.start();
    store.newTransientSnapshot(4, 1).join().persist().join();
    store.newTransientSnapshot(6, 1).join().persist().join();

    assertThat(store.getCompactionBound().join()).isEqualTo(4);
  }

  @Test
  void shouldSkipTakeWhenSameSnapshotIdExists() {
    final AtomicInteger takeCount = new AtomicInteger();
    final var store =
        newStore(
            "node-1",
            1,
            dir -> {
              writeDataFile(dir, "data");
              takeCount.incrementAndGet();
            });
    store.start();
    store.newTransientSnapshot(10, 1).join().persist().join();
    assertThat(takeCount.get()).isEqualTo(1);

    // 相同 id 的拍摄被前置跳过: future 以 null 完成, 不建目录、不触发业务拍摄
    assertThat(store.newTransientSnapshot(10, 1).join()).isNull();
    assertThat(takeCount.get()).isEqualTo(1);
    assertThat(store.getCurrentSnapshotIndex()).isEqualTo(10);
  }

  @Test
  void shouldRejectOlderSnapshotIdOnTake() {
    final var store = newStore("node-1", 1);
    store.start();
    store.newTransientSnapshot(10, 1).join().persist().join();

    // 前置跳过仅适用于相同 id; 已存在更新 id 时拍摄仍异常完成
    assertThatThrownBy(() -> store.newTransientSnapshot(9, 1).join())
        .hasCauseInstanceOf(SnapshotException.SnapshotAlreadyExistsException.class);
  }

  @Test
  void shouldRejectSameOrNewerSnapshotOnReceive() {
    final var store = newStore("node-1", 1);
    store.start();
    store.newTransientSnapshot(10, 1).join().persist().join();

    assertThatThrownBy(() -> store.newReceivedSnapshot(new SnapshotId("node-1", 10, 1).asString()).join())
      .isInstanceOf(Exception.class);
  }

  /** 加载校验与拍摄时的 SnapshotFileInfoProvider 对称：非默认算法（如 rocksdb 原生校验和）拍摄的镜像重启后同样可加载。 */
  @Test
  void shouldReloadSnapshotWithShootTimeProviderChecksums() {
    final SnapshotFileInfoProvider nativeChecksumProvider =
        path -> {
          try {
            return Map.of(
                "data", new SnapshotFileInfo(0x5A5A5A5AL, Files.size(path.resolve("data"))));
          } catch (final IOException e) {
            throw new UncheckedIOException(e);
          }
        };
    final var store = newStoreWithFileInfoProvider("node-1", 1, nativeChecksumProvider);
    store.start();
    store.newTransientSnapshot(7, 2).join().persist().join();

    final var reloaded = newStoreWithFileInfoProvider("node-1", 1, nativeChecksumProvider);
    reloaded.start();

    final var latest = reloaded.getLatestSnapshot();
    assertThat(latest).isPresent();
    assertThat(latest.get().getIndex()).isEqualTo(7);
  }

  private DefaultRaftSnapshotStore newStore(final String nodeId, final int maxSnapshotCount) {
    return newStore(nodeId, maxSnapshotCount, dir -> writeDataFile(dir, "data"));
  }

  /** 指定文件信息提供方构建 store，用于验证加载校验与拍摄算法对称。 */
  private DefaultRaftSnapshotStore newStoreWithFileInfoProvider(
      final String nodeId,
      final int maxSnapshotCount,
      final SnapshotFileInfoProvider fileInfoProvider) {
    return new DefaultRaftSnapshotStore(
        nodeId,
        tmpDir,
        maxSnapshotCount,
        new DefaultSimpleFileVerificationStore(),
        fileInfoProvider,
        new TestSnapshotProvider(
            dir -> {
              writeDataFile(dir, "data");
              return Map.of("key", "value");
            }),
        actor);
  }

  private DefaultRaftSnapshotStore newStore(
    final String nodeId, final int maxSnapshotCount, final SnapshotProviderLike provider) {
    return new DefaultRaftSnapshotStore(
      nodeId,
      tmpDir,
      maxSnapshotCount,
      new DefaultSimpleFileVerificationStore(),
      new DefaultSnapshotFileInfoProvider(),
      new TestSnapshotProvider(
        dir -> {
          provider.take(dir);
          return Map.of("key", "value");
        }),
      actor);
  }

  private static void writeDataFile(final Path dir, final String name) {
    try {
      Files.createDirectories(dir);
      Files.write(dir.resolve(name), "content".getBytes(StandardCharsets.UTF_8));
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static SnapshotChunk chunkOf(final String chunkName, final byte[] content, final long offset) {
    final var crc = new CRC32();
    crc.update(content);
    return new SnapshotChunkImpl(
      chunkName, crc.getValue(), content.length, crc.getValue(), ByteBuffer.wrap(content), offset);
  }

  @FunctionalInterface
  private interface SnapshotProviderLike {
    void take(Path dir) throws Exception;
  }
}
