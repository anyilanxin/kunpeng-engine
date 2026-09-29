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
package com.anyilanxin.kunpeng.cluster.cluster.messaging;

/**
 * 节点通信端口类型注册表：新增独立端口的通信面必须在此登记，并经 {@link MemberNodeInfo} 写入/读取端口映射。
 *
 * <p>端口编码（{@link #code()}）是 {@code member-info-schema} 线上格式的一部分——<b>已分配编码不可变更或复用</b>，新增类型只能追加。
 * 集群面端口即成员地址本身，不进注册表。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum CommPortType {
  /** 业务面（gateway↔broker 业务命令 RPC）。 */
  BUSINESS(1);

  private final int code;

  CommPortType(final int code) {
    this.code = code;
  }

  public int code() {
    return code;
  }

  /** 按线上编码反查；未知编码返回 {@code null}（向前兼容：对端新增类型时本地旧枚举安全忽略）。 */
  public static CommPortType of(final int code) {
    for (final CommPortType type : values()) {
      if (type.code == code) {
        return type;
      }
    }
    return null;
  }
}
