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
package com.anyilanxin.kunpeng.gateway.grpc.utils;

import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.structpack.value.DocumentValue;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import tools.jackson.core.JacksonException;

/**
 * 请求参数里的 JSON 字符串到 MessagePack 缓冲区的转换工具。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class RequestUtil {

  private RequestUtil() {}

  public static DirectBuffer ensureJsonSet(final String value) {
    if (value == null) {
      return DocumentValue.EMPTY_DOCUMENT;
    }
    if (value.trim().isEmpty()) {
      return DocumentValue.EMPTY_DOCUMENT;
    }

    try {
      return new UnsafeBuffer(convertToMsgPack(value));
    } catch (final RuntimeException e) {
      throw explain(value, e);
    }
  }

  /** 转换失败时还原出带定位信息的根因异常；其余异常原样上抛。 */
  private static RuntimeException explain(final String value, final RuntimeException e) {
    final Throwable cause = e.getCause();
    if (cause instanceof final JacksonException parseException) {
      return JacksonException.wrapWithPath(
          cause, new JacksonException.Reference(parseException, "非法 JSON 值：" + value));
    }
    if (cause instanceof final IllegalArgumentException argumentException) {
      return argumentException;
    }
    return e;
  }
}
