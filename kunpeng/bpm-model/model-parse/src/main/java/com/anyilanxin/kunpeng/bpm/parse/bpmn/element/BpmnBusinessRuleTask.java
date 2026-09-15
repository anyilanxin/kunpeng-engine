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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengBindingType;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;

/** 业务规则任务的运行时模型：引用一个 DMN 决策并可选地把结果写入变量。 */
public final class BpmnBusinessRuleTask extends BpmnJobWorkerTask {
  /** 被调用决策的 id 表达式，未声明为 null */
  private ScriptExpression decisionId;

  /** 决策结果写入的变量名，未声明为 null */
  private String resultVariable;

  /** 决策绑定类型（deployment/latest/versionTag），未声明为 null */
  private KunpengBindingType bindingType;

  /** 版本标签（绑定类型为 versionTag 时使用），未声明为 null */
  private String versionTag;

  /** 决策历史数据存活时长，未声明为 null */
  private String historyTimeToLive;

  /**
   * 以元素 id 构造业务规则任务。
   *
   * @param id 元素唯一标识
   */
  public BpmnBusinessRuleTask(final String id) {
    super(id);
  }

  /** 获取被调用决策 id 表达式，未声明时返回 null。 */
  public ScriptExpression getDecisionId() {
    return decisionId;
  }

  /**
   * 设置被调用决策 id 表达式。
   *
   * @param decisionId 决策 id 表达式
   */
  public void setDecisionId(final ScriptExpression decisionId) {
    this.decisionId = decisionId;
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

  /** 获取决策绑定类型，未声明时返回 null。 */
  public KunpengBindingType getBindingType() {
    return bindingType;
  }

  /**
   * 设置决策绑定类型。
   *
   * @param bindingType 绑定类型
   */
  public void setBindingType(final KunpengBindingType bindingType) {
    this.bindingType = bindingType;
  }

  /** 获取版本标签，未声明时返回 null。 */
  public String getVersionTag() {
    return versionTag;
  }

  /**
   * 设置版本标签。
   *
   * @param versionTag 版本标签
   */
  public void setVersionTag(final String versionTag) {
    this.versionTag = versionTag;
  }

  /** 获取决策历史数据存活时长，未声明时返回 null。 */
  public String getHistoryTimeToLive() {
    return historyTimeToLive;
  }

  /**
   * 设置决策历史数据存活时长。
   *
   * @param historyTimeToLive 存活时长
   */
  public void setHistoryTimeToLive(final String historyTimeToLive) {
    this.historyTimeToLive = historyTimeToLive;
  }
}
