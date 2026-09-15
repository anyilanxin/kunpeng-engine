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

/** 消息的运行时模型：消息名与关联键以表达式承载；消息名表达式为静态时可提前解析为常量。 */
public final class BpmnMessage extends BpmnFlowElement {
  /** 消息名表达式 */
  private ScriptExpression messageNameExpression;

  /** 已解析的静态消息名（表达式与变量上下文无关时提前求值），未解析为 null */
  private String messageName;

  /** 订阅关联键表达式（kunpeng:subscription），未声明为 null */
  private ScriptExpression correlationKeyExpression;

  /**
   * 以消息 id 构造消息。
   *
   * @param id 消息唯一标识
   */
  public BpmnMessage(final String id) {
    super(id);
  }

  /** 获取消息名表达式。 */
  public ScriptExpression getMessageNameExpression() {
    return messageNameExpression;
  }

  /**
   * 设置消息名表达式。
   *
   * @param messageNameExpression 消息名表达式
   */
  public void setMessageNameExpression(final ScriptExpression messageNameExpression) {
    this.messageNameExpression = messageNameExpression;
  }

  /** 获取静态消息名，未解析时返回 null。 */
  public String getMessageName() {
    return messageName;
  }

  /**
   * 设置静态消息名（表达式求值成功且与上下文无关时）。
   *
   * @param messageName 静态消息名
   */
  public void setMessageName(final String messageName) {
    this.messageName = messageName;
  }

  /** 获取订阅关联键表达式，未声明时返回 null。 */
  public ScriptExpression getCorrelationKeyExpression() {
    return correlationKeyExpression;
  }

  /**
   * 设置订阅关联键表达式。
   *
   * @param correlationKeyExpression 关联键表达式
   */
  public void setCorrelationKeyExpression(final ScriptExpression correlationKeyExpression) {
    this.correlationKeyExpression = correlationKeyExpression;
  }
}
