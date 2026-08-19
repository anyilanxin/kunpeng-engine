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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStoreFactory;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Path;

/** 快照存储工厂 */
public final class VaultSnapshotStoreFactory implements ReceivableSnapshotStoreFactory {

  private final MeterRegistry meterRegistry;

  public VaultSnapshotStoreFactory() {
    this(null);
  }

  public VaultSnapshotStoreFactory(final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public ReceivableSnapshotStore createReceivableSnapshotStore(
      final Path dataDirectory, final int partitionId) {
    final var vault = new SnapshotVault(dataDirectory, null, meterRegistry);
    return new VaultSnapshotStore(vault);
  }
}
