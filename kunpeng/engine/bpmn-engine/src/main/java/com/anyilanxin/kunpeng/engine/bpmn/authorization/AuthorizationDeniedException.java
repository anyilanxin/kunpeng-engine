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
package com.anyilanxin.kunpeng.engine.bpmn.authorization;

/**
 * 授权拒绝异常：校验不通过时抛出，处理器捕获后以 {@link #CODE} 错误码回写错误响应，客户端凭 403 识别权限问题。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class AuthorizationDeniedException extends RuntimeException {
  /** 权限拒绝错误码（对齐 HTTP 语义，取消处理器 404 先例） */
  public static final int CODE = 403;

  public AuthorizationDeniedException(final String message) {
    super(message);
  }
}
