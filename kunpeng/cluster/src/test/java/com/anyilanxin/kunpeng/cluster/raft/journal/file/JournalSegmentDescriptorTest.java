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
package com.anyilanxin.kunpeng.cluster.raft.journal.file;

import com.anyilanxin.kunpeng.cluster.raft.journal.file.record.CorruptedLogException;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalSegmentDescriptorTest {

  @Test
  void shouldWriteAndReadDescriptor() {
    // given
    JournalSegmentDescriptor descriptor =
      JournalSegmentDescriptor.builder()
        .withId(2)
        .withIndex(100)
        .withMaxSegmentSize(1024)
        .build();
    final ByteBuffer buffer = ByteBuffer.allocate(JournalSegmentDescriptor.getEncodingLength());
    descriptor = descriptor.copyTo(buffer);

    // when
    final JournalSegmentDescriptor descriptorRead = new JournalSegmentDescriptor(buffer);

    // then
    assertThat(descriptorRead).isEqualTo(descriptor);
    assertThat(descriptorRead.id()).isEqualTo(2);
    assertThat(descriptorRead.index()).isEqualTo(100);
    assertThat(descriptorRead.maxSegmentSize()).isEqualTo(1024);
    assertThat(descriptorRead.length()).isEqualTo(JournalSegmentDescriptor.getEncodingLength());
  }

  @Test
  void shouldValidateDescriptorHeader() {
    // given
    final ByteBuffer buffer = ByteBuffer.allocate(JournalSegmentDescriptor.getEncodingLength());

    // when/then
    assertThatThrownBy(() -> new JournalSegmentDescriptor(buffer))
      .isInstanceOf(CorruptedLogException.class);
  }

  @Test
  void shouldReadV1Message() {
    // given
    final ByteBuffer buffer = ByteBuffer.allocate(JournalSegmentDescriptor.getEncodingLength());
    final MutableDirectBuffer directBuffer = new UnsafeBuffer(buffer);
    final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    final SegmentDescriptorEncoder descriptorEncoder = new SegmentDescriptorEncoder();

    directBuffer.putByte(0, (byte) 1);
    descriptorEncoder
      .wrapAndApplyHeader(directBuffer, 1, headerEncoder)
      .id(123)
      .index(456)
      .maxSegmentSize(789);

    // when
    final JournalSegmentDescriptor descriptor = new JournalSegmentDescriptor(buffer);

    // then
    assertThat(descriptor.id()).isEqualTo(123);
    assertThat(descriptor.index()).isEqualTo(456);
    assertThat(descriptor.maxSegmentSize()).isEqualTo(789);
  }

  @Test
  void shouldFailWithChecksumMismatch() {
    // given
    final ByteBuffer buffer = ByteBuffer.allocate(JournalSegmentDescriptor.getEncodingLength());
    final JournalSegmentDescriptor descriptor =
      JournalSegmentDescriptor.builder()
        .withId(123)
        .withIndex(456)
        .withMaxSegmentSize(789)
        .build();
    descriptor.copyTo(buffer);

    // when
    final byte corruptByte = (byte) ~buffer.get(buffer.capacity() - 1);
    buffer.put(buffer.capacity() - 1, corruptByte);

    // then
    assertThatThrownBy(() -> new JournalSegmentDescriptor(buffer))
      .isInstanceOf(CorruptedLogException.class);
  }
}
