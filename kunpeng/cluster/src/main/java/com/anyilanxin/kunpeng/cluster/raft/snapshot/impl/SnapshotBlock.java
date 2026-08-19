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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.VaultCodec.Reader;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.VaultCodec.VaultCodecException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.VaultCodec.Writer;

import java.util.zip.CRC32C;

/**
 * 传输块：快照按 (文件名, 偏移) 切成的自校验帧（vault codec tagged，替代 SBE 帧）。
 *
 * @param snapshotId 所属快照目录名
 * @param blockCount 该快照总块数（接收端完整性判定）
 * @param blockName 块标识 "{fileName}:{offset}"
 * @param crc payload 的 CRC32C
 * @param payload 块字节
 * @param fileOffset 写入目标文件的偏移
 * @param fileSize 目标文件总长（接收端按需扩展）
 */
public record SnapshotBlock(
    String snapshotId,
    int blockCount,
    String blockName,
    long crc,
    byte[] payload,
    long fileOffset,
    long fileSize) {

  private static final int VERSION = 1;

  public SnapshotBlock {
    payload = payload.clone();
  }

  /** 构造并对 payload 现算 CRC32C */
  public static SnapshotBlock of(
      final String snapshotId,
      final int blockCount,
      final String fileName,
      final long fileOffset,
      final long fileSize,
      final byte[] payload) {
    final CRC32C crc = new CRC32C();
    crc.update(payload, 0, payload.length);
    return new SnapshotBlock(
        snapshotId,
        blockCount,
        fileName + ':' + fileOffset,
        crc.getValue(),
        payload,
        fileOffset,
        fileSize);
  }

  @Override
  public byte[] payload() {
    return payload.clone();
  }

  /**
   * @return payload 实算 CRC 与帧内值一致
   */
  public boolean verifyCrc() {
    final CRC32C crc = new CRC32C();
    crc.update(payload, 0, payload.length);
    return crc.getValue() == this.crc;
  }

  public byte[] encode() {
    final var writer = new Writer();
    writer.writeByte(VERSION);
    VaultCodec.putString(writer, snapshotId);
    VaultCodec.putLong(writer, blockCount);
    VaultCodec.putString(writer, blockName);
    VaultCodec.putLong(writer, crc);
    VaultCodec.putBlob(writer, payload);
    VaultCodec.putLong(writer, fileOffset);
    VaultCodec.putLong(writer, fileSize);
    return writer.toByteArray();
  }

  public static SnapshotBlock decode(final byte[] bytes) {
    final var reader = new Reader(bytes);
    final int version = reader.readByte();
    if (version != VERSION) {
      throw new VaultCodecException("快照块帧版本不支持: " + version);
    }
    final String snapshotId = VaultCodec.nextString(reader);
    final int blockCount = (int) VaultCodec.nextLong(reader);
    final String blockName = VaultCodec.nextString(reader);
    final long crc = VaultCodec.nextLong(reader);
    final byte[] payload = VaultCodec.nextBlob(reader);
    final long fileOffset = VaultCodec.nextLong(reader);
    final long fileSize = VaultCodec.nextLong(reader);
    VaultCodec.expectEnd(reader);
    return new SnapshotBlock(snapshotId, blockCount, blockName, crc, payload, fileOffset, fileSize);
  }
}
