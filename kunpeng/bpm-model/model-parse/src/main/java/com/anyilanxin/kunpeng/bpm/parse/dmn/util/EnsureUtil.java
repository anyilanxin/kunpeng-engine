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
 * 参数校验工具类，提供参数非空与类型匹配校验的静态方法，校验失败时抛出 {@link
 * IllegalArgumentException}。
 *
 * @author Stefan Hentschel.
 */
public class EnsureUtil {

  private static final EnsureUtilLogger LOG = UtilsLogger.ENSURE_UTIL_LOGGER;

  /**
   * 确保参数不为 null。
   *
   * @param parameterName 参数名
   * @param value 需要确保不为 null 的值
   * @throws IllegalArgumentException 若参数值为 null
   */
  public static void ensureNotNull(final String parameterName, final Object value) {
    if (value == null) {
      throw LOG.parameterIsNullException(parameterName);
    }
  }

  /**
   * 确保对象为给定类型并返回转换后的对象
   *
   * @param objectName 参数名
   * @param object 参数值
   * @param type 期望的类型
   * @return 转换为请求类型后的参数
   * @throws IllegalArgumentException 若对象无法转换为目标类型
   */
  @SuppressWarnings("unchecked")
  public static <T> T ensureParamInstanceOf(
      final String objectName, final Object object, final Class<T> type) {
    if (type.isAssignableFrom(object.getClass())) {
      return (T) object;
    } else {
      throw LOG.unsupportedParameterType(objectName, object, type);
    }
  }
}
