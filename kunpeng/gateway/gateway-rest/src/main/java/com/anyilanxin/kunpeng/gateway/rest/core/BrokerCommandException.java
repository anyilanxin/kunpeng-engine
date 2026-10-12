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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.rest.core;

/**
 * broker 拒绝命令的异常载体：失败码与提示原样透传给 REST 调用方。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BrokerCommandException extends RuntimeException {
  private static final int DEFAULT_REJECT_CODE = 500;
  private final int code;

  public BrokerCommandException(final int code, final String message) {
    super(message);
    // broker 逻辑拒绝时失败码可能未置位(=0)，归一为非 0，避免产出 code=0 且 success=false 的矛盾信封
    this.code = code > 0 ? code : DEFAULT_REJECT_CODE;
  }

  public int getCode() {
    return code;
  }
}
