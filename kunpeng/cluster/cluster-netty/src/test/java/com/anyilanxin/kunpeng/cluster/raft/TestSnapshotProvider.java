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
package com.anyilanxin.kunpeng.cluster.raft;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.nio.file.Path;
import java.util.Map;

/**
 * 测试用镜像拍摄 SPI 实现：拍摄动作委托给构造时传入的内容写入函数， recover/合并为空完成，目录/store 访问器仅做赋值。
 */
public final class TestSnapshotProvider implements RaftSnapshotProvider<Void> {

  /** 内容写入函数：向拍摄目录写文件并返回业务信息键值清单。 */
  @FunctionalInterface
  public interface SnapshotTaker {
    Map<String, Object> take(Path snapshotDirectory) throws Exception;
  }

  private final SnapshotTaker taker;
  private Path partitionDirectory;
  private Path runtimeDirectory;
  private SnapshotStore snapshotStore;

  public TestSnapshotProvider(final SnapshotTaker taker) {
    this.taker = taker;
  }

  @Override
  public Map<String, Object> takeSnapshot(final Path snapshotDirectory) {
    try {
      return taker.take(snapshotDirectory);
    } catch (final Exception e) {
      throw new IllegalStateException("Test snapshot take failed", e);
    }
  }

  /** 测试无恢复语义，直接空完成。 */
  @Override
  public ActorFuture<Void> recover() {
    return CompletableActorFuture.completed(null);
  }

  /** 测试无合并语义，直接空完成。 */
  @Override
  public ActorFuture<Void> mergeSnapshot(final Path snapshotDirectory) {
    return CompletableActorFuture.completed(null);
  }

  @Override
  public void setPartitionDirectory(final Path partitionDirectory) {
    this.partitionDirectory = partitionDirectory;
  }

  @Override
  public Path getPartitionDirectory() {
    return partitionDirectory;
  }

  @Override
  public void setRuntimeDirectory(final Path runtimeDirectory) {
    this.runtimeDirectory = runtimeDirectory;
  }

  @Override
  public Path getRuntimeDirectory() {
    return runtimeDirectory;
  }

  @Override
  public void setSnapshotStore(final SnapshotStore snapshotStore) {
    this.snapshotStore = snapshotStore;
  }

  @Override
  public SnapshotStore getSnapshotStore() {
    return snapshotStore;
  }

  @Override
  public void close() {}
}
