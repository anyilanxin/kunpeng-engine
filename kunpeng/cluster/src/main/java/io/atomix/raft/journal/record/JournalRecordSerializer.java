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

import io.atomix.utils.Either;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter;
import java.nio.BufferOverflowException;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/** Serializes and deserializes journal records to and from segment buffers. */
public interface JournalRecordSerializer {

  /**
   * Writes the given record's data section to the buffer.
   *
   * @param record whose data section is written
   * @param buffer target of the write
   * @param offset position in the buffer at which writing starts
   * @return either a {@link BufferOverflowException} when the record does not fit, or the number
   *     of bytes written
   */
  default Either<BufferOverflowException, Integer> writeData(
      final RecordData record, final MutableDirectBuffer buffer, final int offset) {
    return writeData(
        record.index(), record.asqn(), DirectBufferWriter.writerFor(record.data()), buffer, offset);
  }

  /** Writes a record data section from the given payload writer. */
  Either<BufferOverflowException, Integer> writeData(
      long index, long asqn, BufferWriter recordDataWriter, MutableDirectBuffer writeBuffer,
      int offset);

  /**
   * Writes the record using the given format version; used when replicating records that were
   * serialized by an older peer.
   */
  Either<BufferOverflowException, Integer> writeDataAtVersion(
      int version, long index, long asqn, BufferWriter recordDataWriter,
      MutableDirectBuffer writeBuffer, int offset);

  /**
   * Writes the given metadata section.
   *
   * @return the number of bytes written, which is always {@link #getMetadataLength()}
   */
  int writeMetadata(RecordMetadata metadata, MutableDirectBuffer buffer, int offset);

  /** @return the constant size of a serialized metadata section */
  int getMetadataLength();

  /**
   * Reads a metadata section.
   *
   * <p>A valid frame must exist at {@code offset}; implementations throw {@link
   * io.atomix.raft.journal.CorruptedJournalException} otherwise.
   */
  RecordMetadata readMetadata(DirectBuffer buffer, int offset);

  /**
   * Reads a record data section.
   *
   * <p>A valid frame must exist at {@code offset}; implementations throw {@link
   * io.atomix.raft.journal.CorruptedJournalException} otherwise.
   */
  RecordData readData(DirectBuffer buffer, int offset);

  /** @return the size of the serialized metadata section found in the buffer */
  int getMetadataLength(DirectBuffer buffer, int offset);
}
