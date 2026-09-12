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
package com.anyilanxin.kunpeng.cluster.raft.storage.system;

import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.BusinessMetaDecoder;
import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.BusinessMetaEncoder;
import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.MessageHeaderDecoder;
import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.utils.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分区业务元数据文件存储（{@code {prefix}.state}），与 {@code *.meta}/{@code *.conf} 同目录。
 *
 * <p>文件是已提交 busimeta 条目的投影：每次应用整体重写（内容自带 index/term）；写失败仅告警， 可由日志重放或快照恢复重建。内容为 1 字节 VERSION + SBE
 * {@code BusinessMeta} 消息 （与 {@code *.conf} 存 Configuration 同模式）。
 */
public final class BusinessMetaStore implements AutoCloseable {
  public static final int VERSION_LENGTH = Byte.BYTES;
  public static final byte VERSION = 1;
  private static final Logger LOGGER = LoggerFactory.getLogger(BusinessMetaStore.class);

  private final FileChannel channel;
  private final File file;

  private BusinessMetaStore(final FileChannel channel, final File file) {
    this.channel = channel;
    this.file = file;
  }

  /** 打开（不存在则创建）分区目录下的 {@code {prefix}.state} 文件。 */
  public static BusinessMetaStore open(final File directory, final String prefix)
      throws IOException {
    FileUtil.ensureDirectory(directory.toPath());
    final File file = new File(directory, String.format("%s.state", prefix));
    final FileChannel channel =
        FileChannel.open(
            file.toPath(),
            StandardOpenOption.CREATE,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE,
            StandardOpenOption.DSYNC);
    fsyncDirectory(directory);
    return new BusinessMetaStore(channel, file);
  }

  /** 文件路径（快照与诊断使用）。 */
  public File file() {
    return file;
  }

  /**
   * 已提交业务元数据状态（记录携带的已提交条目 index/term）。
   *
   * <p>value 为 null 时编码为空串（解码侧得到 ""），调用方不应写入 null value。
   */
  public record BusinessMetaState(long index, long term, Map<String, String> entries) {}

  /** 编码为字节（文件与快照内嵌共用）：1 字节 VERSION + SBE BusinessMeta 消息。 */
  public static byte[] encode(
      final long index, final long term, final Map<String, String> entries) {
    final var buffer = new ExpandableArrayBuffer(256);
    buffer.putByte(0, VERSION);
    final var encoder = new BusinessMetaEncoder();
    final var header = new MessageHeaderEncoder();
    encoder.wrapAndApplyHeader(buffer, VERSION_LENGTH, header);
    encoder.index(index).term(term);
    final var items = encoder.itemsCount(entries.size());
    for (final var entry : entries.entrySet()) {
      final byte[] key = entry.getKey().getBytes(StandardCharsets.UTF_8);
      final byte[] value =
          entry.getValue() == null
              ? new byte[0]
              : entry.getValue().getBytes(StandardCharsets.UTF_8);
      items.next().putKey(key, 0, key.length).putValue(value, 0, value.length);
    }
    final int totalLength = VERSION_LENGTH + header.encodedLength() + encoder.encodedLength();
    final byte[] out = new byte[totalLength];
    buffer.getBytes(0, out);
    return out;
  }

  /** 解码：内容损坏/过短返回 empty（投影可由日志重放重建，不视为致命错误）。 */
  public static Optional<BusinessMetaState> decode(final byte[] data) {
    if (data == null || data.length <= VERSION_LENGTH) {
      return Optional.empty();
    }
    try {
      if (data[0] != VERSION) {
        LOGGER.warn("Unsupported business meta version {}, treating as empty", data[0]);
        return Optional.empty();
      }
      final var decoder = new BusinessMetaDecoder();
      final var header = new MessageHeaderDecoder();
      decoder.wrapAndApplyHeader(new UnsafeBuffer(data), VERSION_LENGTH, header);
      final long index = decoder.index();
      final long term = decoder.term();
      final BusinessMetaDecoder.ItemsDecoder items = decoder.items();
      final Map<String, String> entries = new HashMap<>(items.count());
      for (final BusinessMetaDecoder.ItemsDecoder item : items) {
        final byte[] key = new byte[item.keyLength()];
        item.getKey(key, 0, key.length);
        final byte[] value = new byte[item.valueLength()];
        item.getValue(value, 0, value.length);
        entries.put(
            new String(key, StandardCharsets.UTF_8), new String(value, StandardCharsets.UTF_8));
      }
      return Optional.of(new BusinessMetaState(index, term, entries));
    } catch (final Exception e) {
      LOGGER.warn("Failed to decode business meta data, treating as empty", e);
      return Optional.empty();
    }
  }

  /** 整体重写文件并强制落盘。 */
  public synchronized void store(
      final long index, final long term, final Map<String, String> entries) throws IOException {
    final byte[] data = encode(index, term, entries);
    final ByteBuffer buffer = ByteBuffer.wrap(data);
    channel.position(0);
    while (buffer.hasRemaining()) {
      channel.write(buffer);
    }
    channel.truncate(data.length);
    channel.force(true);
  }

  /** 加载已持久化状态：空/损坏文件返回 empty（由日志重放或快照重建）。 */
  public synchronized Optional<BusinessMetaState> load() throws IOException {
    final long size = channel.size();
    if (size <= VERSION_LENGTH) {
      return Optional.empty();
    }
    final ByteBuffer data = ByteBuffer.allocate((int) size);
    channel.position(0);
    while (data.hasRemaining()) {
      if (channel.read(data) < 0) {
        break;
      }
    }
    return decode(data.array());
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }

  /** 目录条目落盘（参照 MetaStore：失败仅记录，不阻断启动）。 */
  private static void fsyncDirectory(final File directory) throws IOException {
    try (final FileChannel dirChannel =
        FileChannel.open(directory.toPath(), StandardOpenOption.READ)) {
      dirChannel.force(true);
    } catch (final IOException e) {
      LOGGER.debug("Failed to fsync directory {}", directory, e);
    }
  }
}
