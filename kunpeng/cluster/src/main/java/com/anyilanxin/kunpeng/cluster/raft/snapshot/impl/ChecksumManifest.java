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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.VaultCodec.VaultCodecException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32C;

/**
 * 落档快照的逐文件校验清单（SFV 风格文本，一行一条：相对路径 + 校验和）。
 *
 * <p>格式：首行为 {@code ;} 引导的注释头，之后每行
 * {@code 文件相对路径<三空格>校验和字符串}；校验和以字符串存储以适配不同校验算法
 * （CRC32C hex、SHA-256 hex 等）。
 * 综合校验 = 按文件名排序后对 (utf8 名字节 + 校验和字符串 utf8 字节) 序列做一次 CRC32C。
 */
public final class ChecksumManifest {

  private static final String FILE_HEADER = "; Simple file verification - auto generated, do not modify.";
  private static final String ENTRY_SEPARATOR = "   ";

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

  /** SFV 文本编码（UTF-8，一行一条） */
  public byte[] encode() {
    final var text = new StringBuilder();
    text.append(FILE_HEADER).append('\n');
    for (final Map.Entry<String, String> entry : entries.entrySet()) {
      text.append(entry.getKey())
          .append(ENTRY_SEPARATOR)
          .append(entry.getValue())
          .append('\n');
    }
    return text.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * 解析 SFV 文本清单：跳过 {@code ;} 注释行与空行；
   * 每行以最后一段连续空白分隔，前段为相对路径（允许含空格），末段为校验和。
   */
  public static ChecksumManifest decode(final byte[] bytes) {
    final TreeMap<String, String> loaded = new TreeMap<>();
    final String[] lines = new String(bytes, StandardCharsets.UTF_8).split("\n", -1);
    for (final String rawLine : lines) {
      final String line = rawLine.stripTrailing();
      if (line.isEmpty() || line.startsWith(";")) {
        continue;
      }
      final int split = startOfTrailingToken(line);
      final String name = line.substring(0, split).stripTrailing();
      final String checksum = line.substring(split).strip();
      if (name.isEmpty() || checksum.isEmpty()) {
        throw new VaultCodecException("checksum 清单行格式不符: " + line);
      }
      loaded.put(name, checksum);
    }
    return new ChecksumManifest(loaded);
  }

  /** 末段非空白 token 的起始下标（其前至少存在一段空白分隔） */
  private static int startOfTrailingToken(final String line) {
    int end = line.length();
    while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
      end--;
    }
    int start = end;
    while (start > 0 && !Character.isWhitespace(line.charAt(start - 1))) {
      start--;
    }
    return start;
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
