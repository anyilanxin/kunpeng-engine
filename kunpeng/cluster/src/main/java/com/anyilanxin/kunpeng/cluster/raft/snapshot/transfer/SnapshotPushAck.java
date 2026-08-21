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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** 合并快照推送应答：成功/失败与失败原因。线格式（大端）：{@code [成功 1B][原因长度 4B][原因 UTF-8]}。 */
public final class SnapshotPushAck {

  private final boolean ok;
  private final String error;

  private SnapshotPushAck(final boolean ok, final String error) {
    this.ok = ok;
    this.error = error;
  }

  /** 成功应答。 */
  public static SnapshotPushAck ok() {
    return new SnapshotPushAck(true, null);
  }

  /** 失败应答。 */
  public static SnapshotPushAck fail(final String error) {
    return new SnapshotPushAck(false, error);
  }

  public boolean isOk() {
    return ok;
  }

  /** 失败原因；成功时为 null。 */
  public String getError() {
    return error;
  }

  /** 按线格式编码。 */
  public byte[] encode() {
    final byte[] errorBytes =
        error == null ? new byte[0] : error.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES + errorBytes.length);
    buffer.put((byte) (ok ? 1 : 0));
    buffer.putInt(errorBytes.length);
    buffer.put(errorBytes);
    return buffer.array();
  }

  /** 从线格式解码。 */
  public static SnapshotPushAck decode(final byte[] frame) {
    final ByteBuffer buffer = ByteBuffer.wrap(frame);
    final boolean ok = buffer.get() != 0;
    final int length = buffer.getInt();
    if (length < 0 || buffer.remaining() < length) {
      throw new IllegalArgumentException("Malformed push ack length: " + length);
    }
    final byte[] errorBytes = new byte[length];
    buffer.get(errorBytes);
    return new SnapshotPushAck(ok, length == 0 ? null : new String(errorBytes, StandardCharsets.UTF_8));
  }
}
