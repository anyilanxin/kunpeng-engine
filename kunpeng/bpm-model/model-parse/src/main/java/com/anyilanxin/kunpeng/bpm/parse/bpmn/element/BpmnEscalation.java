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

/** 升级的运行时模型：升级码以表达式承载，静态表达式可提前解析为常量。 */
public final class BpmnEscalation extends BpmnFlowElement {
  /** 已解析的静态升级码（含 escalationRef 缺省时的空串），未解析为 null */
  private String escalationCode;

  /** 升级码表达式，未声明为 null */
  private ScriptExpression escalationCodeExpression;

  /**
   * 以升级 id 构造升级。
   *
   * @param id 升级唯一标识
   */
  public BpmnEscalation(final String id) {
    super(id);
  }

  /** 获取静态升级码，未解析时返回 null。 */
  public String getEscalationCode() {
    return escalationCode;
  }

  /**
   * 设置静态升级码。
   *
   * @param escalationCode 静态升级码
   */
  public void setEscalationCode(final String escalationCode) {
    this.escalationCode = escalationCode;
  }

  /** 获取升级码表达式，未声明时返回 null。 */
  public ScriptExpression getEscalationCodeExpression() {
    return escalationCodeExpression;
  }

  /**
   * 设置升级码表达式。
   *
   * @param escalationCodeExpression 升级码表达式
   */
  public void setEscalationCodeExpression(final ScriptExpression escalationCodeExpression) {
    this.escalationCodeExpression = escalationCodeExpression;
  }
}
