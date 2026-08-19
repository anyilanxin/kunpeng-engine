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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 传输块读取器：文件名字典序遍历，按 maxBlockBytes 切块；{@link #seek(String)} 以
 * "{fileName}:{offset}" 游标续传（与 LeaderAppender 超时重发衔接）。
 */
public final class BlockStreamReader {

  private final String snapshotId;
  private final Path directory;
  private final int maxBlockBytes;

  private final List<FileSpan> spans = new ArrayList<>();
  private int spanIndex;
  private long offsetInSpan;
  private int totalBlocks = -1;

  BlockStreamReader(
      final String snapshotId,
      final Path directory,
      final ChecksumManifest manifest,
      final int maxBlockBytes) {
    this.snapshotId = snapshotId;
    this.directory = directory;
    this.maxBlockBytes = maxBlockBytes;
    for (final String name : manifest.entries().keySet()) {
      final Path file = directory.resolve(name);
      final long size;
      try {
        size = Files.size(file);
      } catch (final IOException e) {
        throw new SnapshotStoreException.WriteFailure("块读取器无法定位文件: " + name, e);
      }
      spans.add(new FileSpan(name, file, size));
    }
  }

  /** 该快照总块数（首次调用时计算） */
  public int totalBlocks() {
    if (totalBlocks < 0) {
      long blocks = 0;
      for (final FileSpan span : spans) {
        // 空文件也产一个零长块: 接收端据此重建空文件并纳入清单
        blocks += span.size == 0 ? 1 : (span.size + maxBlockBytes - 1) / maxBlockBytes;
      }
      totalBlocks = (int) blocks;
    }
    return totalBlocks;
  }

  /** 定位到游标（"{fileName}:{offset}"）；未知游标抛 NotFound */
  public void seek(final String blockName) {
    final int separator = blockName.lastIndexOf(':');
    if (separator < 0) {
      throw new SnapshotStoreException.NotFound("块游标格式不符: " + blockName);
    }
    final String fileName = blockName.substring(0, separator);
    final long offset = Long.parseLong(blockName.substring(separator + 1));
    for (int i = 0; i < spans.size(); i++) {
      final FileSpan span = spans.get(i);
      if (span.name.equals(fileName)) {
        spanIndex = i;
        offsetInSpan = offset;
        return;
      }
    }
    throw new SnapshotStoreException.NotFound("块游标指向未知文件: " + blockName);
  }

  /** 当前游标（与 next() 返回块的 blockName 一致） */
  public String cursor() {
    final FileSpan span = spans.get(spanIndex);
    return span.name + ':' + offsetInSpan;
  }

  public boolean hasNext() {
    return spanIndex < spans.size();
  }

  public SnapshotBlock next() {
    if (!hasNext()) {
      throw new NoSuchElementException("快照块已读完");
    }
    final FileSpan span = spans.get(spanIndex);
    final int length = (int) Math.min(maxBlockBytes, span.size - offsetInSpan);
    if (length < 0) {
      throw new IllegalStateException("游标越界: " + span.name + ':' + offsetInSpan);
    }
    final byte[] payload = new byte[length];
    try (RandomAccessFile file = new RandomAccessFile(span.file.toFile(), "r")) {
      file.seek(offsetInSpan);
      file.readFully(payload);
    } catch (final IOException e) {
      throw new SnapshotStoreException.WriteFailure("读取快照块失败: " + cursor(), e);
    }
    final var block =
        SnapshotBlock.of(snapshotId, totalBlocks(), span.name, offsetInSpan, span.size, payload);
    advance(span);
    return block;
  }

  private void advance(final FileSpan span) {
    offsetInSpan += maxBlockBytes;
    if (offsetInSpan >= span.size) {
      spanIndex++;
      offsetInSpan = 0;
    }
  }

  public Path directory() {
    return directory;
  }

  private record FileSpan(String name, Path file, long size) {}
}
