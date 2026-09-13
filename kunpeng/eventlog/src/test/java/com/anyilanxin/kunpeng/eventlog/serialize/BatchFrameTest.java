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
package com.anyilanxin.kunpeng.eventlog.serialize;

import static com.anyilanxin.kunpeng.eventlog.TestEntries.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** VarInt + 批帧编解码测试（字节级规范锚点） */
@DisplayName("VarInt 与批帧编解码")
class BatchFrameTest {

  @Test
  @DisplayName("varint 往返与静态长度一致（含边界值）")
  void varIntRoundTrip() {
    final long[] values = {0, 1, 127, 128, 300, 16383, 16384, Integer.MAX_VALUE,
        4294967295L, Long.MAX_VALUE, -1, -33, -128, -129, -32768, -32769,
        Integer.MIN_VALUE, Long.MIN_VALUE};
    final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(256);
    int offset = 0;
    for (final long value : values) {
      offset = VarInt.writeInt64(buffer, offset, value);
    }
    final VarInt.Cursor cursor = new VarInt.Cursor(0);
    for (final long value : values) {
      assertThat(VarInt.readInt64(buffer, cursor)).isEqualTo(value);
    }
    int expectedLength = 0;
    for (final long value : values) {
      expectedLength += VarInt.int64Length(value);
    }
    assertThat(expectedLength).isEqualTo(offset);
  }

  @Test
  @DisplayName("帧头字节布局（与规范文档一致）")
  void headerLayout() {
    final List<AppendEntry> entries = List.of(entry(-1, -1, false, "m", "v"));
    final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(64);
    final int end = BatchFrame.serialize(buffer, 0, 1, -1, 1500, entries);
    assertThat(buffer.getByte(0)).isEqualTo((byte) 0x45); // 'E'
    assertThat(buffer.getByte(1)).isEqualTo((byte) 0x4C); // 'L'
    assertThat(buffer.getByte(2)).isEqualTo((byte) 0x01); // version
    assertThat(buffer.getByte(3)).isEqualTo((byte) 0); // flags 预留
    assertThat(end)
        .isEqualTo(BatchFrame.calculateLength(1, -1, 1500, entries));
  }

  @Test
  @DisplayName("1..100 条目往返：全部字段逐项断言")
  void roundTrip() {
    for (final int n : new int[] {1, 2, 7, 100}) {
      final List<AppendEntry> entries = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        entries.add(entry(i % 2 == 0 ? -1 : 42L + i, i % 3 == 0 ? i - 1 : -1, i % 5 == 0,
            "meta-" + i, "value-" + i + "-" + "x".repeat(i % 17)));
      }
      final long firstPosition = 1_000_000_000_000L; // 多字节 varint position
      final long timestamp = 1_755_000_000_000L;
      final int length = BatchFrame.calculateLength(firstPosition, -1, timestamp, entries);
      final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(length);
      final int end = BatchFrame.serialize(buffer, 0, firstPosition, -1, timestamp, entries);
      assertThat(end).isEqualTo(length);

      final BatchFrameDecoder decoder =
          new BatchFrameDecoder().wrap(new UnsafeBuffer(buffer, 0, length));
      assertThat(decoder.firstPosition()).isEqualTo(firstPosition);
      assertThat(decoder.entryCount()).isEqualTo(n);
      assertThat(decoder.timestamp()).isEqualTo(timestamp);
      for (int i = 0; i < n; i++) {
        assertThat(decoder.nextEntry()).as("entry %d", i).isTrue();
        assertThat(decoder.entryPosition()).isEqualTo(firstPosition + i);
        assertThat(decoder.entryKey()).isEqualTo(entries.get(i).key());
        assertThat(decoder.entrySkipProcessing()).isEqualTo(entries.get(i).isSkipProcessing());
        final byte[] metadata = new byte[decoder.entryMetadataLength()];
        buffer.getBytes(decoder.entryMetadataOffset(), metadata);
        assertThat(new String(metadata, StandardCharsets.UTF_8)).isEqualTo("meta-" + i);
        final byte[] value = new byte[decoder.entryValueLength()];
        buffer.getBytes(decoder.entryValueOffset(), value);
        assertThat(new String(value, StandardCharsets.UTF_8))
            .isEqualTo("value-" + i + "-" + "x".repeat(i % 17));
      }
      assertThat(decoder.nextEntry()).isFalse();
    }
  }

  @Test
  @DisplayName("sourceIndex 还原为绝对 sourcePosition")
  void sourceIndexResolution() {
    final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(128);
    BatchFrame.serialize(buffer, 0, 100, 77, 5,
        List.of(entry(1, -1, false, "a", "b"), entry(2, 0, false, "c", "d")));
    final BatchFrameDecoder decoder =
        new BatchFrameDecoder().wrap(new UnsafeBuffer(buffer));
    decoder.nextEntry();
    assertThat(decoder.entrySourcePosition()).isEqualTo(77); // 批级
    decoder.nextEntry();
    assertThat(decoder.entrySourcePosition()).isEqualTo(100); // firstPosition + 0
  }

  @Test
  @DisplayName("坏 magic / 版本 / 长度 fail-fast")
  void corruptRejected() {
    // 注: UnsafeBuffer(buffer,off,len) 是共享内存视图, 每个用例须独立序列化新帧
    assertThatThrownBy(() -> new BatchFrameDecoder()
        .wrap(new UnsafeBuffer(freshFrame(), 0, 1)))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("magic");

    final UnsafeBuffer badMagic = freshFrame();
    badMagic.putByte(0, (byte) 'X');
    assertThatThrownBy(() -> new BatchFrameDecoder().wrap(badMagic))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("magic");

    final UnsafeBuffer badVersion = freshFrame();
    badVersion.putByte(2, (byte) 9);
    assertThatThrownBy(() -> new BatchFrameDecoder().wrap(badVersion))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("版本");

    final UnsafeBuffer badLength = freshFrame();
    badLength.putByte(4, (byte) 0x7F); // batchLength 低位改大 → 越界
    assertThatThrownBy(() -> new BatchFrameDecoder().wrap(badLength))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("越界");
  }

  private static UnsafeBuffer freshFrame() {
    final List<AppendEntry> entries = List.of(entry(1, -1, false, "m", "v"));
    final int len = BatchFrame.calculateLength(1, -1, 1, entries);
    final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(len);
    BatchFrame.serialize(buffer, 0, 1, -1, 1, entries);
    return new UnsafeBuffer(buffer.byteArray(), 0, len);
  }

  @Test
  @DisplayName("截断帧拒绝")
  void truncatedRejected() {
    final List<AppendEntry> entries = List.of(entry(1, -1, false, "meta", "value"));
    final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(64);
    final int len = BatchFrame.serialize(buffer, 0, 1, -1, 1, entries);
    for (int cut = 4; cut < len; cut++) {
      final DirectBuffer truncated = new UnsafeBuffer(buffer, 0, cut);
      assertThatThrownBy(() -> new BatchFrameDecoder().wrap(truncated))
          .as("截断至 %d 应失败", cut)
          .isInstanceOf(Exception.class);
    }
  }
}
