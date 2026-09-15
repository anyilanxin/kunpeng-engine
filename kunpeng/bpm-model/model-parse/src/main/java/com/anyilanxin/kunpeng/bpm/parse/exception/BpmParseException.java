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
package com.anyilanxin.kunpeng.bpm.parse.exception;

/** BPM 模型解析模块的基础异常，DMN 与 BPMN 解析期的业务异常均继承本类。 */
public class BpmParseException extends RuntimeException {

  /**
   * 以错误消息构造解析异常。
   *
   * @param message 错误消息
   */
  public BpmParseException(final String message) {
    super(message);
  }

  /**
   * 以错误消息与原因构造解析异常。
   *
   * @param message 错误消息
   * @param cause 原因
   */
  public BpmParseException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
