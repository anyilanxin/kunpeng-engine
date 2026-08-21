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
package io.atomix.raft.journal.record;

import io.atomix.raft.journal.CorruptedJournalException;
import java.nio.ByteOrder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * Codec for the fixed-size metadata frame that precedes every record on disk.
 *
 * <p>Layout (little-endian):
 *
 * <pre>
 * | int32 magic | int8 version | int64 checksum | int32 length |
 * </pre>
 *
 * <p>{@code length} is the size, in bytes, of the serialized {@link RecordData} frame that follows
 * this metadata frame.
 */
final class RecordMetadataCodec {

  static final int MAGIC = 0x4A_52_4D_54; // "JRMT"
  static final int VERSION = 1;
  static final int FRAME_LENGTH = Integer.BYTES + Byte.BYTES + Long.BYTES + Integer.BYTES;

  private static final int OFFSET_MAGIC = 0;
  private static final int OFFSET_VERSION = Integer.BYTES;
  private static final int OFFSET_CHECKSUM = OFFSET_VERSION + Byte.BYTES;
  private static final int OFFSET_LENGTH = OFFSET_CHECKSUM + Long.BYTES;

  private static final ByteOrder ENDIANNESS = ByteOrder.LITTLE_ENDIAN;

  private RecordMetadataCodec() {}

  /** Writes the metadata frame at the given offset and returns the number of bytes written. */
  static int write(
      final MutableDirectBuffer buffer, final int offset, final long checksum, final int length) {
    buffer.putInt(offset + OFFSET_MAGIC, MAGIC, ENDIANNESS);
    buffer.putByte(offset + OFFSET_VERSION, (byte) VERSION);
    buffer.putLong(offset + OFFSET_CHECKSUM, checksum, ENDIANNESS);
    buffer.putInt(offset + OFFSET_LENGTH, length, ENDIANNESS);
    return FRAME_LENGTH;
  }

  /** Reads and validates the metadata frame at the given offset. */
  static RecordMetadata read(final DirectBuffer buffer, final int offset) {
    validate(buffer, offset);
    return new RecordMetadata(
        buffer.getLong(offset + OFFSET_CHECKSUM, ENDIANNESS),
        buffer.getInt(offset + OFFSET_LENGTH, ENDIANNESS));
  }

  /** Ensures the frame carries the expected magic value and a supported version. */
  static void validate(final DirectBuffer buffer, final int offset) {
    if (buffer.getInt(offset + OFFSET_MAGIC, ENDIANNESS) != MAGIC) {
      throw new CorruptedJournalException(
          "Cannot read record metadata: unexpected magic value; the record is likely corrupt");
    }
    final int version = buffer.getByte(offset + OFFSET_VERSION);
    if (version > VERSION) {
      throw new CorruptedJournalException(
          String.format(
              "Cannot read record metadata: version %d is newer than the supported version %d",
              version, VERSION));
    }
  }
}
