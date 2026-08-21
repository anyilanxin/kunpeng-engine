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

interface SegmentDescriptorSerializer {

  byte CUR_VERSION = 3;
  byte[] SUPPORTED_VERSIONS = new byte[] {CUR_VERSION};

  static short currentEncodingLength() {
    return (short) BinarySegmentDescriptorSerializer.ENCODING_LENGTH;
  }

  /** @return the major on-disk format version written by this serializer */
  byte majorVersion();

  /** @return the minor format version, i.e. forward/backward compatible schema revisions */
  byte minorVersion();

  /** @return the fixed number of bytes a descriptor occupies at the head of a segment file */
  int encodingLength();

  /**
   * Writes the given descriptor into the buffer.
   *
   * @param segmentDescriptor the descriptor to write
   * @param buffer the buffer to write to
   */
  void writeTo(SegmentDescriptor segmentDescriptor, ByteBuffer buffer);

  /**
   * Reads a descriptor from the buffer.
   *
   * @param buffer to read from
   * @return the segment descriptor
   * @throws UnknownVersionException if the version is not compatible with this serializer
   * @throws com.anyilanxin.kunpeng.cluster.raft.journal.CorruptedJournalException if the checksum check fails
   */
  SegmentDescriptor readFrom(ByteBuffer buffer);

  static SegmentDescriptorSerializer currentSerializer() {
    return forVersion(CUR_VERSION);
  }

  static SegmentDescriptorSerializer forVersion(final byte version) {
    return switch (version) {
      case CUR_VERSION -> new BinarySegmentDescriptorSerializer();
      default ->
          throw new IllegalArgumentException(
              "Version %d is not supported. Supported versions are %s"
                  .formatted(version, Arrays.toString(SUPPORTED_VERSIONS)));
    };
  }
}
