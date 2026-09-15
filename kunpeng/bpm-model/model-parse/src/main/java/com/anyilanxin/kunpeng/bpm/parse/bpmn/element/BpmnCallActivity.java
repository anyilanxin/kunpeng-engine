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

/** 调用活动的运行时模型：以表达式引用被调用流程并声明变量传递策略与绑定方式。 */
public final class BpmnCallActivity extends BpmnActivity {
  /** 被调用流程 id 的表达式 */
  private ScriptExpression calledElementProcessId;

  /** 是否向父流程传播全部子流程变量 */
  private boolean propagateAllChildVariables;

  /** 是否向子流程传播全部父流程变量 */
  private boolean propagateAllParentVariables;

  /** 绑定类型（deployment/latest/versionTag），未声明为 null */
  private KunpengBindingType bindingType;

  /** 版本标签（绑定类型为 versionTag 时使用），未声明为 null */
  private String versionTag;

  /** 本调用活动 id 在同一部署资源内全部调用活动 id 按字典序排序后的位置（转换完成后统一计算）。 */
  private int lexicographicIndex = -1;

  /**
   * 以元素 id 构造调用活动。
   *
   * @param id 元素唯一标识
   */
  public BpmnCallActivity(final String id) {
    super(id);
  }

  /** 获取被调用流程 id 表达式。 */
  public ScriptExpression getCalledElementProcessId() {
    return calledElementProcessId;
  }

  /**
   * 设置被调用流程 id 表达式。
   *
   * @param calledElementProcessId 流程 id 表达式
   */
  public void setCalledElementProcessId(final ScriptExpression calledElementProcessId) {
    this.calledElementProcessId = calledElementProcessId;
  }

  /** 是否向父流程传播全部子流程变量。 */
  public boolean isPropagateAllChildVariables() {
    return propagateAllChildVariables;
  }

  /**
   * 设置是否向父流程传播全部子流程变量。
   *
   * @param propagateAllChildVariables 是否传播
   */
  public void setPropagateAllChildVariables(final boolean propagateAllChildVariables) {
    this.propagateAllChildVariables = propagateAllChildVariables;
  }

  /** 是否向子流程传播全部父流程变量。 */
  public boolean isPropagateAllParentVariables() {
    return propagateAllParentVariables;
  }

  /**
   * 设置是否向子流程传播全部父流程变量。
   *
   * @param propagateAllParentVariables 是否传播
   */
  public void setPropagateAllParentVariables(final boolean propagateAllParentVariables) {
    this.propagateAllParentVariables = propagateAllParentVariables;
  }

  /** 获取绑定类型，未声明时返回 null。 */
  public KunpengBindingType getBindingType() {
    return bindingType;
  }

  /**
   * 设置绑定类型。
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

  /** 获取字典序索引（转换完成后填充，未计算时为 -1）。 */
  public int getLexicographicIndex() {
    return lexicographicIndex;
  }

  /**
   * 设置字典序索引。
   *
   * @param lexicographicIndex 字典序索引
   */
  public void setLexicographicIndex(final int lexicographicIndex) {
    this.lexicographicIndex = lexicographicIndex;
  }
}
