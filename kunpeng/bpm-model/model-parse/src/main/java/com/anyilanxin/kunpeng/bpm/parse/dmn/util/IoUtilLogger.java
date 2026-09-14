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

import com.anyilanxin.kunpeng.bpm.parse.dmn.exception.IoUtilException;
import java.io.IOException;

/**
 * IoUtil 的日志工具类，定义 IO 操作失败时构造 {@link IoUtilException} 的工厂方法。
 *
 * @author Sebastian Menski
 */
public class IoUtilLogger extends UtilsLogger {

  /**
   * 构造“读取输入流失败”的 IoUtil 异常。
   *
   * @param cause 读取失败的原因
   * @return 读取输入流失败的 IoUtil 异常
   */
  public IoUtilException unableToReadInputStream(final IOException cause) {
    return new IoUtilException(exceptionMessage("001", "Unable to read input stream"), cause);
  }

  /**
   * 构造“找不到指定路径文件”的 IoUtil 异常。
   *
   * @param filename 文件路径
   * @param cause 失败原因
   * @return 找不到文件的 IoUtil 异常
   */
  public IoUtilException fileNotFoundException(final String filename, final Exception cause) {
    return new IoUtilException(
        exceptionMessage("002", "Unable to find file with path '{}'", filename), cause);
  }

  /**
   * 构造“找不到指定路径文件”的 IoUtil 异常（不带原因）。
   *
   * @param filename 文件路径
   * @return 找不到文件的 IoUtil 异常
   */
  public IoUtilException fileNotFoundException(final String filename) {
    return fileNotFoundException(filename, null);
  }

  /**
   * 构造“参数为 null”的 IoUtil 异常。
   *
   * @param parameter 参数名
   * @return 参数为 null 的 IoUtil 异常
   */
  public IoUtilException nullParameter(final String parameter) {
    return new IoUtilException(
        exceptionMessage("003", "Parameter '{}' can not be null", parameter));
  }

  /**
   * 构造“从 Reader 读取内容失败”的 IoUtil 异常。
   *
   * @param cause 读取失败的原因
   * @return 读取失败的 IoUtil 异常
   */
  public IoUtilException unableToReadFromReader(final Throwable cause) {
    return new IoUtilException(exceptionMessage("004", "Unable to read from reader"), cause);
  }
}
