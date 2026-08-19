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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.v2.VaultCodec.Reader;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.v2.VaultCodec.Writer;

/** 快照元数据（目录内 snapshot.metadata，vault codec 编码） */
public final class SnapshotMeta {

  private static final int VERSION = 1;

  private final long processedPosition;
  private final long followupEventPosition;
  private final boolean bootstrap;
  private final long exportedPosition;

  public SnapshotMeta(
      final long processedPosition,
      final long followupEventPosition,
      final boolean bootstrap,
      final long exportedPosition) {
    this.processedPosition = processedPosition;
    this.followupEventPosition = followupEventPosition;
    this.bootstrap = bootstrap;
    this.exportedPosition = exportedPosition;
  }

  public long processedPosition() {
    return processedPosition;
  }

  public long followupEventPosition() {
    return followupEventPosition;
  }

  public boolean bootstrap() {
    return bootstrap;
  }

  public long exportedPosition() {
    return exportedPosition;
  }

  public byte[] encode() {
    final var writer = new Writer();
    writer.writeByte(VERSION);
    VaultCodec.putLong(writer, processedPosition);
    VaultCodec.putLong(writer, followupEventPosition);
    VaultCodec.putLong(writer, bootstrap ? 1 : 0);
    VaultCodec.putLong(writer, exportedPosition);
    return writer.toByteArray();
  }

  public static SnapshotMeta decode(final byte[] bytes) {
    final var reader = new Reader(bytes);
    final int version = reader.readByte();
    if (version != VERSION) {
      throw new VaultCodec.VaultCodecException("snapshot.metadata 版本不支持: " + version);
    }
    final long processed = VaultCodec.nextLong(reader);
    final long followup = VaultCodec.nextLong(reader);
    final boolean bootstrap = VaultCodec.nextLong(reader) == 1;
    final long exported = VaultCodec.nextLong(reader);
    VaultCodec.expectEnd(reader);
    return new SnapshotMeta(processed, followup, bootstrap, exported);
  }
}
