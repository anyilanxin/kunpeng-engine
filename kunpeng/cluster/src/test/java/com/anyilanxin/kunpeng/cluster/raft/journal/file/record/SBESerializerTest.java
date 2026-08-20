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
package com.anyilanxin.kunpeng.cluster.raft.journal.file.record;

import com.anyilanxin.kunpeng.utils.Either;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SBESerializerTest {

  private RecordData record;
  private RecordMetadata metadata;
  private SBESerializer serializer;
  private MutableDirectBuffer writeBuffer;

  @BeforeEach
  public void setup() {
    serializer = new SBESerializer();

    final DirectBuffer data = new UnsafeBuffer();
    data.wrap("firstData".getBytes());
    record = new RecordData(1, 2, data);

    metadata = new RecordMetadata(1L, 2);

    final ByteBuffer buffer = ByteBuffer.allocate(256);
    writeBuffer = new UnsafeBuffer(buffer);
  }

  @Test
  public void shouldWriteRecord() {
    // given - when
    final var recordWrittenLength = serializer.writeData(record, writeBuffer, 0).get();

    // then
    assertThat(recordWrittenLength).isPositive();
  }

  @Test
  public void shouldReadRecord() {
    // given
    final var length = serializer.writeData(record, writeBuffer, 0).get();

    // when
    final var recordRead = serializer.readData(writeBuffer, 0, length);

    // then
    assertThat(recordRead.index()).isEqualTo(record.index());
    assertThat(recordRead.asqn()).isEqualTo(record.asqn());
    assertThat(recordRead.data()).isEqualTo(record.data());
  }

  @Test
  public void shouldWriteMetadata() {
    // given - when
    final var metadataLength = serializer.writeMetadata(metadata, writeBuffer, 0);

    // then
    assertThat(metadataLength).isEqualTo(serializer.getMetadataLength());
  }

  @Test
  public void shouldReadMetadata() {
    // given
    serializer.writeMetadata(metadata, writeBuffer, 0);

    // when
    final var metadataRead = serializer.readMetadata(writeBuffer, 0);

    // then
    assertThat(metadataRead.checksum()).isEqualTo(metadata.checksum());
    assertThat(metadataRead.length()).isEqualTo(metadata.length());
  }

  @Test
  public void shouldThrowCorruptLogExceptionIfMetadataIsInvalid() {
    // given
    serializer.writeMetadata(metadata, writeBuffer, 0);
    writeBuffer.putLong(0, 0);

    // when - then
    assertThatThrownBy(() -> serializer.readMetadata(writeBuffer, 0))
      .isInstanceOf(CorruptedLogException.class);
  }

  @Test
  public void shouldThrowExceptionWhenInvalidRecord() {
    // given
    writeBuffer.putLong(0, 0);

    // when - then
    assertThatThrownBy(() -> serializer.readData(writeBuffer, 0, 1))
      .isInstanceOf(CorruptedLogException.class);
  }

  @Test
  public void shouldReadLengthEqualToActualLength() {
    // given
    final int actualMetadataLength = serializer.writeMetadata(metadata, writeBuffer, 0);

    // when
    final int readMetadataLength = serializer.getMetadataLength(writeBuffer, 0);

    // then
    assertThat(readMetadataLength).isEqualTo(actualMetadataLength);
  }

  @Test
  public void shouldWriteRecordAtAnyOffset() {
    // given
    final int offset = 10;

    // when
    final var recordWrittenLength = serializer.writeData(record, writeBuffer, offset).get();
    final var readData = serializer.readData(writeBuffer, offset, recordWrittenLength);

    // then
    assertThat(readData).isEqualTo(record);
  }

  @Test
  public void shouldWriteMetadataAtAnyOffset() {
    // given
    final int offset = 10;

    // when
    serializer.writeMetadata(metadata, writeBuffer, offset);
    final var readMetadata = serializer.readMetadata(writeBuffer, offset);

    // then
    assertThat(readMetadata).isEqualTo(metadata);
  }

  @Test
  public void shouldThrowBufferOverFlowWhenNotEnoughSpace() {
    // given
    final int offset = writeBuffer.capacity() - 1;

    // when - then
    assertThat(serializer.writeData(record, writeBuffer, offset)).matches(Either::isLeft);
  }
}
