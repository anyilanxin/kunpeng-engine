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
package com.anyilanxin.kunpeng.cluster.raft.journal.file;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * segment 文件头部描述符的编解码器。
 *
 * <p>描述符固定占据 segment 文件开头 {@link #encodingLength()} 字节，携带格式版本、segment
 * 编号、首索引与容量上限。每个格式版本对应一个实现；{@link #CUR_VERSION} 是当前写盘版本。
 */
interface SegmentDescriptorSerializer {

  /** 当前写盘的描述符格式版本。 */
  byte CUR_VERSION = 3;

  /** 全部可读取的格式版本。 */
  byte[] SUPPORTED_VERSIONS = new byte[] {CUR_VERSION};

  /**
   * @return 当前格式下描述符的固定字节数
   */
  static short currentEncodingLength() {
    return (short) BinarySegmentDescriptorSerializer.ENCODING_LENGTH;
  }

  /**
   * @return 当前写盘版本对应的编解码器
   */
  static SegmentDescriptorSerializer currentSerializer() {
    return forVersion(CUR_VERSION);
  }

  /**
   * 按磁盘上声明的格式版本取对应编解码器。
   *
   * @throws IllegalArgumentException 版本不在 {@link #SUPPORTED_VERSIONS} 内
   */
  static SegmentDescriptorSerializer forVersion(final byte version) {
    if (version != CUR_VERSION) {
      throw new IllegalArgumentException(
          "不支持的描述符格式版本 %d，可读取的版本为 %s".formatted(version, Arrays.toString(SUPPORTED_VERSIONS)));
    }
    return new BinarySegmentDescriptorSerializer();
  }

  /**
   * @return 该格式的主版本号（不兼容变更时递增）
   */
  byte majorVersion();

  /**
   * @return 该格式的次版本号（向前/向后兼容的结构微调）
   */
  byte minorVersion();

  /**
   * @return 该格式下描述符的固定字节数
   */
  int encodingLength();

  /**
   * 把描述符编码写入 buffer。
   *
   * @param descriptor 待写入的描述符
   * @param buffer 目标缓冲
   */
  void writeTo(SegmentDescriptor descriptor, ByteBuffer buffer);

  /**
   * 从 buffer 解码描述符。
   *
   * @param buffer 来源缓冲
   * @throws UnknownVersionException 声明的格式版本无法识别
   * @throws com.anyilanxin.kunpeng.cluster.raft.journal.CorruptedJournalException 描述符 校验和不一致
   */
  SegmentDescriptor readFrom(ByteBuffer buffer);
}
