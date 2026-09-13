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
package com.anyilanxin.kunpeng.protocol.admin;

/**
 * @author zxuanhong
 * @since
 */
public enum AdminValueType {
  // 协议值必须全局唯一：AdminValueType.valueOf(short) 按声明顺序返回第一个匹配，
  // 重复值会导致 RecordMetadata 往返解码后 valueType 串类型
  UNKNOW((short) -1),
  // 部署相关
  ADMIN_DISPATCH_EXECUTION((short) 0),
  ADMIN_DISPATCH_API((short) 1),
  ADMIN_DISPATCH((short) 2),

  BUSINESS_DISPATCH_EXECUTION((short) 3),
  BUSINESS_DISPATCH_API((short) 4),
  BUSINESS_DISPATCH((short) 5),

  ADMIN_CLUSTER_META((short) 6),
  BUSINESS_CLUSTER_META((short) 7),

  DELAYED((short) 8),

  NODE_SOURCE_META((short) 9),
  NODE_SOURCE((short) 10),
  PARTITION_SOURCE_META((short) 11),
  PARTITION_SOURCE((short) 12),
  ;

  private final short value;

  AdminValueType(final short value) {
    this.value = value;
  }

  public short getValue() {
    return value;
  }

  public static AdminValueType valueOf(final short value) {
    for (final AdminValueType type : AdminValueType.values()) {
      if (type.value == value) {
        return type;
      }
    }
    return UNKNOW;
  }
}
