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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.rocksdb;

import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionStep;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.AdminTransitionContent;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryColumnFamilies;
import com.anyilanxin.kunpeng.repository.admin.RocksdbAdminRepositoryFactory;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.google.common.collect.ImmutableSet;

public final class RocksdbPartitionTransitionStep
    implements TransitionStep<AdminTransitionContent> {

  @Override
  public String getName() {
    return "Admin Rocksdb Partition Transition";
  }

  @Override
  public ActorFuture<Void> onLeader(final AdminTransitionContent context, final long currentTerm) {
    final ConcurrencyControl concurrencyControl = context.getConcurrencyControl();
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    recoverDb(context, future);
    return future;
  }

  @Override
  public ActorFuture<Void> onFollower(
      final AdminTransitionContent context, final long currentTerm) {
    final ConcurrencyControl concurrencyControl = context.getConcurrencyControl();
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    recoverDb(context, future);
    return future;
  }

  @Override
  public ActorFuture<Void> onInactive(
      final AdminTransitionContent context, final long currentTerm) {
    final RaftSnapshotProvider<KvStore<AdminRepositoryColumnFamilies>> snapshotProvider =
        context.getSnapshotProvider();
    final ConcurrencyControl concurrencyControl = context.getConcurrencyControl();
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(
        () -> {
          snapshotProvider.close();
          context.setRocksdb(null);
          context.setRepositoryFactory(null);
          future.complete(null);
        });
    return future;
  }

  private void recoverDb(
      final AdminTransitionContent context, final ActorFuture<Void> transitionFuture) {
    final ActorFuture<KvStore<AdminRepositoryColumnFamilies>> recoverFuture;
    if (context.getRocksdb() == null) {
      recoverFuture = context.getSnapshotProvider().recover();
      recoverFuture.onComplete(
          (db, error) -> {
            if (error != null) {
              transitionFuture.completeExceptionally(
                  new IllegalStateException("recover failed: " + error.getMessage()));
            } else {
              context.setRocksdb(db);
              context.setRepositoryFactory(
                  new RocksdbAdminRepositoryFactory(
                      db,
                      new PartitionSourceMetadata(1, 1, ImmutableSet.<Integer>builder().build()),
                      context.getMeterRegistry()));
              transitionFuture.complete(null);
            }
          });
    } else {
      transitionFuture.complete(null);
    }
  }
}
