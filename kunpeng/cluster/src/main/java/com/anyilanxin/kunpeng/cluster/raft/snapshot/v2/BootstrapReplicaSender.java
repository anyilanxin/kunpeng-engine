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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.v2;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.v2.ReplicaSenderService.SnapshotTaker;
import com.anyilanxin.kunpeng.utils.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.utils.scheduler.future.CompletableActorFuture;
import java.util.Optional;

/** bootstrap 副本发送器（bootstrap-snapshots 缓存区） */
public final class BootstrapReplicaSender extends ReplicaTransferHub {

  public BootstrapReplicaSender(
      final SnapshotVault vault, final SnapshotTaker taker, final int maxBlockBytes) {
    super(vault, taker, maxBlockBytes);
  }

  @Override
  protected Optional<ArchivedSnapshot> currentCached() {
    return vault().getBootstrapSnapshot();
  }

  @Override
  protected ActorFuture<Void> deleteCached() {
    return wrapVoid(vault().deleteBootstrapSnapshots().toCompletableFuture());
  }

  /** CompletableFuture 包装为 ActorFuture */
  static ActorFuture<Void> wrapVoid(final java.util.concurrent.CompletableFuture<Void> f) {
    return new CompletableActorFuture<>(f);
  }
}
