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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * An in-progress snapshot which is being taken locally. The caller writes arbitrary files into a
 * temporary directory via {@link #take(Consumer)}, then either {@link #commit()}s it, making it
 * durable and visible, or {@link #abort()}s it, discarding it.
 */
public interface TransientSnapshot {

  /**
   * Invokes the given writer with the temporary directory into which it must write the snapshot's
   * files.
   *
   * @param writer the writer of the snapshot content
   * @return a future completed when writing is done
   */
  CompletableFuture<Void> take(Consumer<Path> writer);

  /**
   * Finalizes the snapshot: verifies the written content, computes checksums, and atomically moves
   * it to its final location.
   *
   * @return a future completed with the persisted snapshot
   */
  CompletableFuture<PersistedSnapshot> commit();

  /** Discards the snapshot and deletes any temporary files. */
  void abort();
}
