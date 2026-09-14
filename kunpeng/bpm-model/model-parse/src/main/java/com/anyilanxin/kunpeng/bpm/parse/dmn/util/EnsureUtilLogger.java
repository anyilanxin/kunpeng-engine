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

package com.anyilanxin.kunpeng.bpm.parse.dmn.util;

/**
 * EnsureUtil 的日志工具类，定义参数校验失败时构造 {@link IllegalArgumentException} 的工厂方法。
 *
 * @author Stefan Hentschel.
 */
public class EnsureUtilLogger extends UtilsLogger {

  /**
   * 构造“参数为 null”的非法参数异常。
   *
   * @param parameterName 参数名
   * @return 参数为 null 的非法参数异常
   */
  public IllegalArgumentException parameterIsNullException(final String parameterName) {
    return new IllegalArgumentException(
        exceptionMessage("001", "Parameter '{}' is null", parameterName));
  }

  /**
   * 构造“参数类型与期望类型不符”的非法参数异常。
   *
   * @param parameterName 参数名
   * @param param 实际参数值
   * @param expectedType 期望的参数类型
   * @return 参数类型不符的非法参数异常
   */
  public IllegalArgumentException unsupportedParameterType(
      final String parameterName, final Object param, final Class<?> expectedType) {
    return new IllegalArgumentException(
        exceptionMessage(
            "002",
            "Unsupported parameter '{}' of type '{}'. Expected type '{}'.",
            parameterName,
            param.getClass(),
            expectedType.getName()));
  }
}
