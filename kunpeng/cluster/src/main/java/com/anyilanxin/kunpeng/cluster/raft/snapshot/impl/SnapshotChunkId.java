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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Utility to render a snapshot chunk id for logging purposes. A chunk id is the UTF-8 encoded
 * {@code <fileName>:<blockNumber>} of the chunk it identifies.
 */
public final class SnapshotChunkId {

  private final ByteBuffer chunkId;

  public SnapshotChunkId(final ByteBuffer chunkId) {
    this.chunkId = Objects.requireNonNull(chunkId, "chunkId must not be null");
  }

  @Override
  public String toString() {
    final ByteBuffer duplicate = chunkId.slice();
    final byte[] bytes = new byte[duplicate.remaining()];
    duplicate.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
