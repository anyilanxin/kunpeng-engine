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

/** 信号的运行时模型：信号名以表达式承载，静态表达式可提前解析为常量。 */
public final class BpmnSignal extends BpmnFlowElement {
  /** 信号名表达式 */
  private ScriptExpression signalNameExpression;

  /** 已解析的静态信号名，未解析为 null */
  private String signalName;

  /**
   * 以信号 id 构造信号。
   *
   * @param id 信号唯一标识
   */
  public BpmnSignal(final String id) {
    super(id);
  }

  /** 获取信号名表达式。 */
  public ScriptExpression getSignalNameExpression() {
    return signalNameExpression;
  }

  /**
   * 设置信号名表达式。
   *
   * @param signalNameExpression 信号名表达式
   */
  public void setSignalNameExpression(final ScriptExpression signalNameExpression) {
    this.signalNameExpression = signalNameExpression;
  }

  /** 获取静态信号名，未解析时返回 null。 */
  public String getSignalName() {
    return signalName;
  }

  /**
   * 设置静态信号名。
   *
   * @param signalName 静态信号名
   */
  public void setSignalName(final String signalName) {
    this.signalName = signalName;
  }
}
