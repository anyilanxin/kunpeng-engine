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
package com.anyilanxin.kunpeng.cluster.manager.business.raft;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.nio.file.Path;
import java.util.Map;

/**
 * 业务分区 Raft 快照提供者，负责业务分区快照内容的构建与写出（当前实现返回空快照）。
 *
 * @author zxuanhong
 * @since
 */
public class BusinessRaftSnapshotProvider
    implements RaftSnapshotProvider<KvStore<AdminRepositoryColumnFamilies>> {
  @Override
  public Map<String, Object> takeSnapshot(final Path snapshotDirectory) {
    return Map.of();
  }

  @Override
  public void setPartitionDirectory(final Path partitionDirectory) {}

  @Override
  public Path getPartitionDirectory() {
    return null;
  }

  @Override
  public void setRuntimeDirectory(final Path runtimeDirectory) {}

  @Override
  public Path getRuntimeDirectory() {
    return null;
  }

  @Override
  public void setSnapshotStore(final SnapshotStore snapshotStore) {}

  @Override
  public SnapshotStore getSnapshotStore() {
    return null;
  }

  @Override
  public ActorFuture<KvStore<AdminRepositoryColumnFamilies>> recover() {
    return null;
  }

  @Override
  public void close() {}

  @Override
  public ActorFuture<Void> mergeSnapshot(final Path snapshotDirectory) {
    return null;
  }
}
