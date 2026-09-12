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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

/**
 * 持久镜像的分片迭代器：{@link #nextId()} 预览下一个分片 id 而不消费，{@link #next()} 返回分片 并推进。分片 id 即 chunkName（{@code
 * 文件名@字节偏移}）的 UTF-8 字节，作为续传 token。 读取失败（镜像被删或损坏）抛 {@link UncheckedIOException}。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface SnapshotChunkReader extends AutoCloseable {

  /** 是否还有下一个分片。 */
  boolean hasNext();

  /** 下一个分片的 id（不消费）；耗尽时返回 {@code null}。 */
  ByteBuffer nextId();

  /**
   * 返回下一个分片并推进。
   *
   * @throws java.util.NoSuchElementException 已耗尽
   */
  SnapshotChunk next();

  /** 定位到指定分片 id，使下一次 {@link #next()} 返回它。 */
  void seek(ByteBuffer chunkId);

  /** 重置到第一个分片。 */
  void reset();

  /** 设置后续分片的最大内容尺寸（字节），对之后的所有分片生效。 */
  void setMaximumChunkSize(int maximumChunkSize);

  /** 关闭读取器释放资源；可重复调用。 */
  @Override
  void close();
}
