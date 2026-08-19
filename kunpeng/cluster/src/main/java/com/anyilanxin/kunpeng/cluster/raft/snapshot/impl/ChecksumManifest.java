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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.VaultCodec.Reader;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.VaultCodec.VaultCodecException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.VaultCodec.Writer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32C;

/**
 * 落档快照的逐文件校验清单（vault codec 二进制 manifest）。
 *
 * <p>校验和以字符串存储以适配不同校验算法（CRC32C hex、SHA-256 hex 等）。
 * 综合校验 = 按文件名排序后对 (utf8 名字节 + 校验和字符串 utf8 字节) 序列做一次 CRC32C。
 */
public final class ChecksumManifest {

  private static final int VERSION = 2;

  private final TreeMap<String, String> entries;
  private long combined;

  private ChecksumManifest(final TreeMap<String, String> entries) {
    this.entries = entries;
    combined = computeCombined(entries);
  }

  public static ChecksumManifest empty() {
    return new ChecksumManifest(new TreeMap<>());
  }

  /** 追加一条（构建期） */
  public void add(final String fileName, final String checksum) {
    entries.put(fileName, checksum);
    combined = computeCombined(entries);
  }

  /** 文件校验和；不存在返回 {@code null} */
  public String checksumOf(final String fileName) {
    return entries.get(fileName);
  }

  public long combined() {
    return combined;
  }

  /** 按文件名排序的条目 */
  public Map<String, String> entries() {
    return java.util.Collections.unmodifiableMap(entries);
  }

  public int size() {
    return entries.size();
  }

  /** 全部条目与综合值逐项相等 */
  public boolean matches(final ChecksumManifest other) {
    return combined == other.combined && entries.equals(other.entries);
  }

  public byte[] encode() {
    final var writer = new Writer();
    writer.writeByte(VERSION);
    VaultCodec.putArrayHeader(writer, entries.size());
    for (final Map.Entry<String, String> entry : entries.entrySet()) {
      VaultCodec.putString(writer, entry.getKey());
      VaultCodec.putString(writer, entry.getValue());
    }
    VaultCodec.putLong(writer, combined);
    return writer.toByteArray();
  }

  public static ChecksumManifest decode(final byte[] bytes) {
    final var reader = new Reader(bytes);
    final int version = reader.readByte();
    if (version != VERSION) {
      throw new VaultCodecException("manifest 版本不支持: " + version);
    }
    final int count = VaultCodec.nextArrayHeader(reader);
    final TreeMap<String, String> entries = new TreeMap<>();
    for (int i = 0; i < count; i++) {
      final String name = VaultCodec.nextString(reader);
      final String checksum = VaultCodec.nextString(reader);
      entries.put(name, checksum);
    }
    final long storedCombined = VaultCodec.nextLong(reader);
    VaultCodec.expectEnd(reader);
    final var manifest = new ChecksumManifest(entries);
    if (manifest.combined != storedCombined) {
      throw new VaultCodecException("manifest 综合校验不符");
    }
    return manifest;
  }

  static long computeCombined(final TreeMap<String, String> entries) {
    final CRC32C crc = new CRC32C();
    for (final Map.Entry<String, String> entry : entries.entrySet()) {
      crc.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
      crc.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
    }
    return crc.getValue();
  }

  static List<String> sortedNames(final ChecksumManifest manifest) {
    return new ArrayList<>(manifest.entries.keySet());
  }
}
