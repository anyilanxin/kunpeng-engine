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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistableSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkAppender;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 通用文件分片写入器：pending 快照未自实现 {@link SnapshotChunkAppender} 时的退化实现，
 * 直接向 {@link PersistableSnapshot#getPath()} 按 {@code 文件名@偏移} 写入并跟踪 totalCount。
 */
public final class FileSnapshotChunkAppender implements SnapshotChunkAppender {

  private final PersistableSnapshot snapshot;
  // 首个分片声明的总分片数，后续分片必须与其一致
  private final AtomicReference<Integer> expectedTotalCount = new AtomicReference<>();
  // 已接收的不同分片名集合
  private final Set<String> receivedChunkNames = ConcurrentHashMap.newKeySet();

  public FileSnapshotChunkAppender(final PersistableSnapshot snapshot) {
    this.snapshot = snapshot;
  }

  @Override
  public CompletableFuture<Void> append(final SnapshotChunk chunk) {
    return CompletableFuture.runAsync(
        () -> {
          if (snapshot.getPath() == null) {
            throw new SnapshotException(
                "Pending snapshot " + snapshot.snapshotId() + " has no directory to write into");
          }
          if (!chunk.getSnapshotId().equals(snapshot.snapshotId().asString())) {
            throw new SnapshotException(
                "Expected chunk of snapshot " + snapshot.snapshotId().asString()
                    + ", but got " + chunk.getSnapshotId());
          }
          final Integer expected =
              expectedTotalCount.updateAndGet(
                  current -> current == null ? chunk.getTotalCount() : current);
          if (expected != chunk.getTotalCount()) {
            throw new SnapshotException(
                "Expected total chunk count " + expected + ", but got " + chunk.getTotalCount()
                    + " for chunk " + chunk.getChunkName());
          }
          FilePersistableSnapshot.verifyChecksum(chunk);
          FilePersistableSnapshot.writeChunkFile(snapshot.getPath(), chunk);
          receivedChunkNames.add(chunk.getChunkName());
        });
  }

  @Override
  public void verifyComplete() {
    final Integer expectedCount = expectedTotalCount.get();
    if (expectedCount != null && receivedChunkNames.size() != expectedCount) {
      throw new SnapshotException(
          "Received " + receivedChunkNames.size() + " chunks of snapshot " + snapshot.snapshotId()
              + ", but expected " + expectedCount);
    }
  }
}
