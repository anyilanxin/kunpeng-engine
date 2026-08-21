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
package io.atomix.raft;

import io.atomix.raft.storage.log.IndexedRaftLogEntry;
import io.atomix.raft.zeebe.ZeebeLogAppender.AppendListener;
import java.util.concurrent.CompletableFuture;

/**
 * 测试辅助类：把一次日志追加的写入阶段与提交阶段分别暴露为 future，便于在断言中
 * 分别等待“条目已写入”和“条目已提交”两种结果。任一阶段失败时对应 future 会收到异常。
 */
final class AppendResult implements AppendListener {
  private final CompletableFuture<Long> persisted = new CompletableFuture<>();
  private final CompletableFuture<Long> committed = new CompletableFuture<>();

  /** @return 条目成功提交后以条目索引完成的 future；写入或提交失败则异常完成 */
  CompletableFuture<Long> committedIndex() {
    return committed;
  }

  /** @return 条目成功写入磁盘后以条目索引完成的 future；写入失败则异常完成 */
  CompletableFuture<Long> persistedIndex() {
    return persisted;
  }

  @Override
  public void onWrite(final IndexedRaftLogEntry indexed) {
    persisted.complete(indexed.index());
  }

  @Override
  public void onWriteError(final Throwable error) {
    // 写入失败的条目不可能再提交，因此两个 future 一起异常完成
    persisted.completeExceptionally(error);
    committed.completeExceptionally(error);
  }

  @Override
  public void onCommit(final long index, final long highestPosition) {
    committed.complete(index);
  }

  @Override
  public void onCommitError(final long index, final Throwable error) {
    committed.completeExceptionally(error);
  }
}
