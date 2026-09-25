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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.element;

import com.anyilanxin.kunpeng.engine.script.ScriptExpression;

/**
 * 错误的运行时模型：错误码以表达式承载，静态表达式可提前解析为常量。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BpmnError extends BpmnFlowElement {
  /** 已解析的静态错误码（含 errorRef 缺省时的空串），未解析为 null */
  private String errorCode;

  /** 错误码表达式 */
  private ScriptExpression errorCodeExpression;

  /**
   * 以错误 id 构造错误。
   *
   * @param id 错误唯一标识
   */
  public BpmnError(final String id) {
    super(id);
  }

  /** 获取静态错误码，未解析时返回 null。 */
  public String getErrorCode() {
    return errorCode;
  }

  /**
   * 设置静态错误码。
   *
   * @param errorCode 静态错误码
   */
  public void setErrorCode(final String errorCode) {
    this.errorCode = errorCode;
  }

  /** 获取错误码表达式，未声明时返回 null。 */
  public ScriptExpression getErrorCodeExpression() {
    return errorCodeExpression;
  }

  /**
   * 设置错误码表达式。
   *
   * @param errorCodeExpression 错误码表达式
   */
  public void setErrorCodeExpression(final ScriptExpression errorCodeExpression) {
    this.errorCodeExpression = errorCodeExpression;
  }
}
