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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengAdHocImplementationType;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 临时子流程的运行时模型：内部活动可乱序执行，由激活集合表达式与完成条件控制。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BpmnAdHocSubProcess extends BpmnContainer {
  /** 内部实例 id 的固定后缀 */
  private static final String INNER_INSTANCE_ID_POSTFIX = "-inner";

  /** 内部实例 id（流程执行时临时子流程以内部实例形态展开） */
  private final String innerInstanceId;

  /** 激活内部活动的集合表达式 */
  private ScriptExpression activeElementsCollection;

  /** 完成条件表达式 */
  private ScriptExpression completionCondition;

  /** 完成后是否取消未完成的内部实例 */
  private boolean cancelRemainingInstances = true;

  /** 实现类型 */
  private KunpengAdHocImplementationType implementationType;

  /** 内部活动注册表（id -> 活动），懒分配由转换器填充 */
  private Map<String, BpmnFlowNode> adHocActivities;

  /**
   * 以元素 id 构造临时子流程。
   *
   * @param id 元素唯一标识
   */
  public BpmnAdHocSubProcess(final String id) {
    super(id);
    innerInstanceId = id + INNER_INSTANCE_ID_POSTFIX;
  }

  /** 获取内部实例 id。 */
  public String getInnerInstanceId() {
    return innerInstanceId;
  }

  /** 获取激活内部活动的集合表达式，未声明时返回 null。 */
  public ScriptExpression getActiveElementsCollection() {
    return activeElementsCollection;
  }

  /**
   * 设置激活内部活动的集合表达式。
   *
   * @param activeElementsCollection 激活集合表达式
   */
  public void setActiveElementsCollection(final ScriptExpression activeElementsCollection) {
    this.activeElementsCollection = activeElementsCollection;
  }

  /** 获取完成条件表达式，未声明时返回 null。 */
  public ScriptExpression getCompletionCondition() {
    return completionCondition;
  }

  /**
   * 设置完成条件表达式。
   *
   * @param completionCondition 完成条件表达式
   */
  public void setCompletionCondition(final ScriptExpression completionCondition) {
    this.completionCondition = completionCondition;
  }

  /** 完成后是否取消未完成的内部实例。 */
  public boolean isCancelRemainingInstances() {
    return cancelRemainingInstances;
  }

  /**
   * 设置完成后是否取消未完成的内部实例。
   *
   * @param cancelRemainingInstances 是否取消
   */
  public void setCancelRemainingInstances(final boolean cancelRemainingInstances) {
    this.cancelRemainingInstances = cancelRemainingInstances;
  }

  /** 获取实现类型，未声明时返回 null。 */
  public KunpengAdHocImplementationType getImplementationType() {
    return implementationType;
  }

  /**
   * 设置实现类型。
   *
   * @param implementationType 实现类型
   */
  public void setImplementationType(final KunpengAdHocImplementationType implementationType) {
    this.implementationType = implementationType;
  }

  /**
   * 登记内部活动（多实例活动的内部活动会以多实例体覆盖登记）。
   *
   * @param activity 内部活动
   */
  public void addAdHocActivity(final BpmnFlowNode activity) {
    if (adHocActivities == null) {
      adHocActivities = new HashMap<>(8);
    }
    adHocActivities.put(activity.getId(), activity);
  }

  /** 获取内部活动注册表，未登记时返回空映射。 */
  public Map<String, BpmnFlowNode> getAdHocActivities() {
    return adHocActivities == null ? Collections.emptyMap() : adHocActivities;
  }
}
