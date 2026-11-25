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
package com.anyilanxin.kunpeng.broker.client.core;

/**
 * broker 通信异常：请求失败或超时时的统一异常类型。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BrokerException extends Exception {
  /**
   * 目标分区的 leader 解析不出来（选举进行中、成员离线、业务端口未广播）：暂态，命令没有发出，可安全重试。 其余码值表示请求本身无法寻址（无效 key、找不到源分区等），重试无意义。
   */
  public static final int LEADER_UNRESOLVABLE = 11;

  private final int code;

  public BrokerException(final int code, final String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
