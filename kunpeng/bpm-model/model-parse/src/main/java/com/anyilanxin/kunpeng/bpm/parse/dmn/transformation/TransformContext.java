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
package com.anyilanxin.kunpeng.bpm.parse.dmn.transformation;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.*;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DMN 转换上下文：在分阶段转换器之间共享，缓存已转换的运行时元素、决策与业务知识注册表，并携带决策需求图与表达式语言等全局信息。
 */
public final class TransformContext {
  private final Map<String, DmnBusinessKnowledge> businessKnowledgeMap = new HashMap<>();
  private final Map<String, DmnDecision> dmnDecisionMap = new HashMap<>();
  /** 已转换的通用运行时元素注册表（以元素 id 为键） */
  private final Map<String, DmnElement> elementMap = new HashMap<>();
  private DmnDecisionRequirementsGraphImpl requirementsGraph;
  /** 用于把 DMN 表达式编译为可执行脚本的表达式引擎 */
  private ScriptEngine expressionLanguage;
  /** Definitions 上声明的全局表达式语言 */
  private String language;

  /** 获取正在构建的决策需求图。 */
  public DmnDecisionRequirementsGraphImpl getRequirementsGraph() {
    return requirementsGraph;
  }

  /**
   * 设置决策需求图。
   *
   * @param requirementsGraph 决策需求图
   */
  public void setRequirementsGraph(final DmnDecisionRequirementsGraphImpl requirementsGraph) {
    this.requirementsGraph = requirementsGraph;
  }

  /**
   * 注册已转换的运行时元素（以元素 id 为键）。
   *
   * @param element 已转换的运行时元素
   */
  public <T extends DmnElement> void addElement(final T element) {
    elementMap.put(getKey(element), element);
  }

  /**
   * 按元素类型与 id 获取已转换的运行时元素。
   *
   * @param elementType 元素类型（当前实现仅按 id 寻址）
   * @param key 元素 id
   * @return 对应的运行时元素，不存在时返回 null
   */
  public <T extends DmnElement> T getElement(final ElementType elementType, final String key) {
    return (T) elementMap.get(getKey(elementType, key));
  }

  private <T extends DmnElement> String getKey(final T element) {
    return getKey(element.getType(), element.getKey());
  }

  private String getKey(final ElementType elementType, final String key) {
    return key;
  }

  /**
   * 注册已转换的决策（以决策 key 为键）。
   *
   * @param decision 已转换的决策
   */
  public void addDecision(final DmnDecision decision) {
    dmnDecisionMap.put(decision.getKey(), decision);
  }

  /** 按决策 key 获取已转换的决策。 */
  public DmnDecision getDecision(final String key) {
    return dmnDecisionMap.get(key);
  }

  /** 获取全部已转换的决策。 */
  public List<DmnDecision> getDecisions() {
    return new ArrayList<>(dmnDecisionMap.values());
  }

  /**
   * 注册已转换的业务知识（以其 key 为键）。
   *
   * @param businessKnowledge 已转换的业务知识
   */
  public void addBusinessKnowledge(final DmnBusinessKnowledge businessKnowledge) {
    businessKnowledgeMap.put(businessKnowledge.getKey(), businessKnowledge);
  }

  /** 按业务知识 key 获取已转换的业务知识。 */
  public DmnBusinessKnowledge getBusinessKnowledge(final String key) {
    return businessKnowledgeMap.get(key);
  }

  /** 获取表达式编译引擎。 */
  public ScriptEngine getExpressionLanguage() {
    return expressionLanguage;
  }

  /**
   * 设置表达式编译引擎。
   *
   * @param expressionLanguage 表达式编译引擎
   */
  public void setExpressionLanguage(final ScriptEngine expressionLanguage) {
    this.expressionLanguage = expressionLanguage;
  }

  /**
   * 设置全局表达式语言（来自 Definitions 的声明）。
   *
   * @param language 全局表达式语言
   */
  public void setLanguage(final String language) {
    this.language = language;
  }

  /** 获取全局表达式语言。 */
  public String getLanguage() {
    return language;
  }
}
