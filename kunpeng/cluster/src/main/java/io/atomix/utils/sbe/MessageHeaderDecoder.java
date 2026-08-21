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
package io.atomix.utils.sbe;

import org.agrona.DirectBuffer;

/** Standard SBE message header decoder: blockLength u16, templateId u16, schemaId u16, version u16. */
public final class MessageHeaderDecoder {

  public static final int ENCODED_LENGTH = 8;
  public static final int BLOCK_LENGTH = 8;
  public static final int TEMPLATE_ID = 0;
  public static final int SCHEMA_ID = 0;
  public static final int SCHEMA_VERSION = 0;

  private DirectBuffer buffer;
  private int offset;

  public int sbeBlockLength() {
    return BLOCK_LENGTH;
  }

  public int sbeTemplateId() {
    return TEMPLATE_ID;
  }

  public int sbeSchemaId() {
    return SCHEMA_ID;
  }

  public int sbeSchemaVersion() {
    return SCHEMA_VERSION;
  }

  public int sbeEncodedLength() {
    return ENCODED_LENGTH;
  }

  public MessageHeaderDecoder wrap(final DirectBuffer buffer, final int offset) {
    this.buffer = buffer;
    this.offset = offset;
    return this;
  }

  public int encodedLength() {
    return ENCODED_LENGTH;
  }

  public int blockLength() {
    return buffer.getShort(offset + 0, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF;
  }

  public int templateId() {
    return buffer.getShort(offset + 2, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF;
  }

  public int schemaId() {
    return buffer.getShort(offset + 4, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF;
  }

  public int version() {
    return buffer.getShort(offset + 6, java.nio.ByteOrder.LITTLE_ENDIAN) & 0xFFFF;
  }
}
