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
package com.anyilanxin.kunpeng.cluster.raft.impl.zeebe;

import com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Raft 日志压缩器：根据可压缩索引截断日志。
 *
 * <p>仅截断到 {@code min(可压缩索引, 最新快照索引)}。
 */
public final class LogCompactor implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(LogCompactor.class);

  private final RaftContext context;
  private final AtomicLong compactableIndex = new AtomicLong(0);

  public LogCompactor(final RaftContext context) {
    this.context = context;
  }

  /** 设置可压缩索引（上层按已处理位点推进） */
  public void setCompactableIndex(final long index) {
    compactableIndex.set(index);
  }

  /** 截断日志到可压缩索引（幂等） */
  public CompletableFuture<Void> compact() {
    final var future = new CompletableFuture<Void>();
    context.getThreadContext()
        .execute(
            () -> {
              try {
                final var snapshotIndex = context.getCurrentSnapshotIndex();
                final var bound = Math.min(compactableIndex.get(), snapshotIndex);
                final var log = context.getLog();
                if (bound > 0 && bound < log.getLastIndex()) {
                  log.deleteUntil(bound);
                  LOG.debug("Compacted log up to index {} (last={})", bound, log.getLastIndex());
                }
                future.complete(null);
              } catch (final Exception e) {
                LOG.warn("Log compaction failed", e);
                future.completeExceptionally(e);
              }
            });
    return future;
  }

  @Override
  public void close() {
    // 无后台资源需要释放
  }
}
