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
package com.anyilanxin.kunpeng.cluster.raft.journal.record;

import com.anyilanxin.kunpeng.cluster.raft.journal.CorruptedJournalException;
import com.anyilanxin.kunpeng.cluster.utils.Either;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.nio.BufferOverflowException;
import java.nio.ByteOrder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * Codec for the record data frame stored in a journal segment.
 *
 * <p>Layout (little-endian):
 *
 * <pre>
 * | int32 magic | int8 version | int64 index | int64 asqn | int32 length | byte[length] payload |
 * </pre>
 */
final class RecordDataCodec {

  static final int MAGIC = 0x4A_52_44_54; // "JRDT"
  static final int VERSION = 1;
  static final int HEADER_LENGTH =
      Integer.BYTES + Byte.BYTES + Long.BYTES + Long.BYTES + Integer.BYTES;

  private static final int OFFSET_MAGIC = 0;
  private static final int OFFSET_VERSION = Integer.BYTES;
  private static final int OFFSET_INDEX = OFFSET_VERSION + Byte.BYTES;
  private static final int OFFSET_ASQN = OFFSET_INDEX + Long.BYTES;
  private static final int OFFSET_LENGTH = OFFSET_ASQN + Long.BYTES;
  private static final int OFFSET_PAYLOAD = OFFSET_LENGTH + Integer.BYTES;

  private static final ByteOrder ENDIANNESS = ByteOrder.LITTLE_ENDIAN;

  private RecordDataCodec() {}

  /** Total number of bytes occupied by a data frame carrying {@code payloadLength} payload bytes. */
  static int frameLength(final int payloadLength) {
    return HEADER_LENGTH + payloadLength;
  }

  /**
   * Writes the data frame, copying the payload produced by {@code payloadWriter}, at the given
   * offset.
   *
   * @return either an overflow error if the frame does not fit in the buffer, or the number of
   *     bytes written
   */
  static Either<BufferOverflowException, Integer> write(
      final long index,
      final long asqn,
      final BufferWriter payloadWriter,
      final MutableDirectBuffer buffer,
      final int offset) {
    final int length = frameLength(payloadWriter.getLength());
    if (offset + length > buffer.capacity()) {
      return Either.left(new BufferOverflowException());
    }

    buffer.putInt(offset + OFFSET_MAGIC, MAGIC, ENDIANNESS);
    buffer.putByte(offset + OFFSET_VERSION, (byte) VERSION);
    buffer.putLong(offset + OFFSET_INDEX, index, ENDIANNESS);
    buffer.putLong(offset + OFFSET_ASQN, asqn, ENDIANNESS);
    buffer.putInt(offset + OFFSET_LENGTH, payloadWriter.getLength(), ENDIANNESS);
    payloadWriter.write(buffer, offset + OFFSET_PAYLOAD);
    return Either.right(length);
  }

  /** Reads and validates the data frame at the given offset; the payload is returned as a view. */
  static RecordData read(final DirectBuffer buffer, final int offset) {
    if (buffer.getInt(offset + OFFSET_MAGIC, ENDIANNESS) != MAGIC) {
      throw new CorruptedJournalException(
          "Cannot read record data: unexpected magic value; the record is likely corrupt");
    }
    final int version = buffer.getByte(offset + OFFSET_VERSION);
    if (version > VERSION) {
      throw new CorruptedJournalException(
          String.format(
              "Cannot read record data: version %d is newer than the supported version %d",
              version, VERSION));
    }

    final long index = buffer.getLong(offset + OFFSET_INDEX, ENDIANNESS);
    final long asqn = buffer.getLong(offset + OFFSET_ASQN, ENDIANNESS);
    final int length = buffer.getInt(offset + OFFSET_LENGTH, ENDIANNESS);
    final DirectBuffer payload =
        new UnsafeBuffer(buffer, offset + OFFSET_PAYLOAD, Math.max(0, length));
    return new RecordData(index, asqn, payload);
  }
}
