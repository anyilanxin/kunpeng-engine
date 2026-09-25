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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListenerEventType;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 流程元素运行时模型的公共基类。
 *
 * <p>承载元素的标识、名称、文档、类型、所属流程范围以及扩展属性；执行监听器也统一存储在基类，由子类按自身语义提供访问方法。 集合字段均为懒分配，未使用时保持 {@code null}
 * 以最小化单个元素的内存占用。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class BpmnFlowElement {
  /** 元素唯一标识（对应 BPMN 的 id 属性） */
  private final String id;

  /** 元素名称，未声明时为 null */
  private String name;

  /** 元素的第一段文档描述，未声明时为 null */
  private String documentation;

  /** 运行时元素类型，初始为 {@link BpmnElementType#UNSPECIFIED} */
  private BpmnElementType elementType = BpmnElementType.UNSPECIFIED;

  /** 事件语义类型，仅事件相关元素使用，初始为 {@link BpmnEventType#UNSPECIFIED} */
  private BpmnEventType eventType = BpmnEventType.UNSPECIFIED;

  /** 直接父级流程元素（流程本身或某层容器），顶层元素为 null */
  private BpmnFlowElement flowScope;

  /** 扩展属性（kunpeng:properties），未声明时为 null */
  private Map<String, String> properties;

  /** 动态扩展表达式（kunpeng:additions），未声明时为 null */
  private Map<String, ScriptExpression> additions;

  /** 执行监听器，未声明时为 null */
  private List<BpmnExecutionListener> executionListeners;

  /**
   * 以元素 id 构造流程元素。
   *
   * @param id 元素唯一标识
   */
  protected BpmnFlowElement(final String id) {
    this.id = id;
  }

  /** 获取元素唯一标识。 */
  public String getId() {
    return id;
  }

  /** 获取元素名称，未声明时返回 null。 */
  public String getName() {
    return name;
  }

  /**
   * 设置元素名称。
   *
   * @param name 元素名称
   */
  public void setName(final String name) {
    this.name = name;
  }

  /** 获取元素文档描述，未声明时返回 null。 */
  public String getDocumentation() {
    return documentation;
  }

  /**
   * 设置元素文档描述。
   *
   * @param documentation 文档描述
   */
  public void setDocumentation(final String documentation) {
    this.documentation = documentation;
  }

  /** 获取运行时元素类型。 */
  public BpmnElementType getElementType() {
    return elementType;
  }

  /**
   * 设置运行时元素类型。
   *
   * @param elementType 元素类型
   */
  public void setElementType(final BpmnElementType elementType) {
    this.elementType = elementType;
  }

  /** 获取事件语义类型。 */
  public BpmnEventType getEventType() {
    return eventType;
  }

  /**
   * 设置事件语义类型。
   *
   * @param eventType 事件语义类型
   */
  public void setEventType(final BpmnEventType eventType) {
    this.eventType = eventType;
  }

  /** 获取直接父级流程元素，顶层元素返回 null。 */
  public BpmnFlowElement getFlowScope() {
    return flowScope;
  }

  /**
   * 设置直接父级流程元素。
   *
   * @param flowScope 父级流程元素
   */
  public void setFlowScope(final BpmnFlowElement flowScope) {
    this.flowScope = flowScope;
  }

  /** 获取扩展属性，未声明时返回空映射（只读约定，调用方不得修改）。 */
  public Map<String, String> getProperties() {
    return properties == null ? Collections.emptyMap() : properties;
  }

  /**
   * 设置扩展属性。
   *
   * @param properties 扩展属性映射
   */
  public void setProperties(final Map<String, String> properties) {
    this.properties = properties;
  }

  /** 获取动态扩展表达式，未声明时返回空映射（只读约定，调用方不得修改）。 */
  public Map<String, ScriptExpression> getAdditions() {
    return additions == null ? Collections.emptyMap() : additions;
  }

  /**
   * 设置动态扩展表达式。
   *
   * @param additions 动态扩展表达式映射
   */
  public void setAdditions(final Map<String, ScriptExpression> additions) {
    this.additions = additions;
  }

  /** 是否声明了执行监听器。 */
  public boolean hasExecutionListeners() {
    return executionListeners != null && !executionListeners.isEmpty();
  }

  /**
   * 注册执行监听器（监听器声明缺失必要属性时由转换器先行过滤）。
   *
   * @param eventType 监听时机（start/take/end）
   * @param type 任务类型表达式
   * @param retries 重试次数表达式
   */
  public void addExecutionListener(
      final KunpengExecutionListenerEventType eventType,
      final ScriptExpression type,
      final ScriptExpression retries) {
    if (executionListeners == null) {
      executionListeners = new ArrayList<>(2);
    }
    executionListeners.add(new BpmnExecutionListener(eventType, type, retries));
  }

  /** 是否声明了指定时机的执行监听器。 */
  public boolean hasExecutionListener(final KunpengExecutionListenerEventType eventType) {
    if (executionListeners == null) {
      return false;
    }
    for (final BpmnExecutionListener listener : executionListeners) {
      if (listener.getEventType() == eventType) {
        return true;
      }
    }
    return false;
  }

  /**
   * 获取指定时机的执行监听器列表。
   *
   * @param eventType 监听时机（start/take/end）
   * @return 对应时机的监听器列表，未声明时返回空列表
   */
  public List<BpmnExecutionListener> getExecutionListeners(
      final KunpengExecutionListenerEventType eventType) {
    if (executionListeners == null) {
      return Collections.emptyList();
    }
    final List<BpmnExecutionListener> matched = new ArrayList<>(executionListeners.size());
    for (final BpmnExecutionListener listener : executionListeners) {
      if (listener.getEventType() == eventType) {
        matched.add(listener);
      }
    }
    return matched;
  }

  /** 简要描述，用于日志与调试（避免输出整棵流程图导致循环引用）。 */
  @Override
  public String toString() {
    return getClass().getSimpleName() + "{id='" + id + "', name='" + name + "'}";
  }
}
