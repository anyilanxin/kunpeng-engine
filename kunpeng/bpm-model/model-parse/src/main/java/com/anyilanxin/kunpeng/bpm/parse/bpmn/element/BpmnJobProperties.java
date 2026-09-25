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
 * 任务型元素（job worker 元素）的任务属性：任务类型与重试次数，两者均以表达式承载。
 *
 * <p>独立的顶层类（而非嵌套类）便于任务元素与执行监听器共同复用，避免为每个元素复制定义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BpmnJobProperties {
  /** 任务类型表达式 */
  private ScriptExpression type;

  /** 重试次数表达式 */
  private ScriptExpression retries;

  /** 获取任务类型表达式。 */
  public ScriptExpression getType() {
    return type;
  }

  /**
   * 设置任务类型表达式。
   *
   * @param type 任务类型表达式
   */
  public void setType(final ScriptExpression type) {
    this.type = type;
  }

  /** 获取重试次数表达式。 */
  public ScriptExpression getRetries() {
    return retries;
  }

  /**
   * 设置重试次数表达式。
   *
   * @param retries 重试次数表达式
   */
  public void setRetries(final ScriptExpression retries) {
    this.retries = retries;
  }
}
