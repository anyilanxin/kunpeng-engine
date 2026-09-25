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
 * 脚本任务的运行时模型：内联脚本表达式与可选的结果变量名。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BpmnScriptTask extends BpmnJobWorkerTask {
  /** 脚本表达式，未声明为 null */
  private ScriptExpression expression;

  /** 脚本结果写入的变量名，未声明为 null */
  private String resultVariable;

  /**
   * 以元素 id 构造脚本任务。
   *
   * @param id 元素唯一标识
   */
  public BpmnScriptTask(final String id) {
    super(id);
  }

  /** 获取脚本表达式，未声明时返回 null。 */
  public ScriptExpression getExpression() {
    return expression;
  }

  /**
   * 设置脚本表达式。
   *
   * @param expression 脚本表达式
   */
  public void setExpression(final ScriptExpression expression) {
    this.expression = expression;
  }

  /** 获取结果变量名，未声明时返回 null。 */
  public String getResultVariable() {
    return resultVariable;
  }

  /**
   * 设置结果变量名。
   *
   * @param resultVariable 结果变量名
   */
  public void setResultVariable(final String resultVariable) {
    this.resultVariable = resultVariable;
  }
}
