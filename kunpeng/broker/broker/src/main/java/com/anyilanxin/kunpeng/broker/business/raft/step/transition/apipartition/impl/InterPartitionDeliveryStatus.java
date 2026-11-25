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
package com.anyilanxin.kunpeng.broker.business.raft.step.transition.apipartition.impl;

/**
 * 跨分区命令接收方对一条命令的处理结果，也是应答的全部载荷（单字节状态码）。
 *
 * <p>{@link #APPENDED}：命令已写入接收分区的日志（已定序），发送端不必再发。{@link
 * #REJECTED}：接收方明确没有写入（磁盘不可用、日志拒绝写入），命令确定不在日志里，发送端重发是安全的。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
enum InterPartitionDeliveryStatus {
  APPENDED((byte) 0),
  REJECTED((byte) 1);

  private final byte code;

  InterPartitionDeliveryStatus(final byte code) {
    this.code = code;
  }

  static byte[] toBytes(final InterPartitionDeliveryStatus status) {
    return new byte[] {status.code};
  }

  static InterPartitionDeliveryStatus fromBytes(final byte[] bytes) {
    for (final var status : values()) {
      if (status.code == bytes[0]) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown inter-partition delivery status " + bytes[0]);
  }
}
