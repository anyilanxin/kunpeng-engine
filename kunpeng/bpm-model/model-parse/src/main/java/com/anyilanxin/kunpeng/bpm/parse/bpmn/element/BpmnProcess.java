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

import com.anyilanxin.kunpeng.bpm.parse.exception.BpmnParseException;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 流程定义的运行时模型：持有全流程元素注册表（以元素 id 索引）与流程级配置。
 *
 * <p>注册表同时覆盖嵌套子流程中的元素与多实例活动体（多实例体与内部活动共用同一 id，查询时可按期望类型解包）。
 */
public class BpmnProcess extends BpmnContainer {
  /** 全流程元素注册表（id -> 元素），初始容量取常见流程规模减少扩容拷贝 */
  private final Map<String, BpmnFlowElement> flowElements = new HashMap<>(64);

  /** 历史数据存活时长（天），未声明为 null */
  private Integer historyTimeToLive;

  /** 历史数据存活时长的原始表达式，未声明为 null */
  private ScriptExpression historyTimeToLiveExpression;

  /** 是否可执行流程 */
  private boolean executable;

  /**
   * 以流程 id 构造流程定义（流程自身也登记进元素注册表）。
   *
   * @param id 流程 id
   */
  public BpmnProcess(final String id) {
    super(id);
    setElementType(BpmnElementType.PROCESS);
    flowElements.put(id, this);
  }

  /**
   * 登记流程元素（同 id 元素后写覆盖，用于多实例体替换内部活动等场景）。
   *
   * @param element 流程元素
   */
  public void addFlowElement(final BpmnFlowElement element) {
    flowElements.put(element.getId(), element);
  }

  /**
   * 按元素 id 与期望类型查询流程元素。
   *
   * <p>当命中多实例活动体而期望类型是内部活动类型时，自动解包为内部活动。
   *
   * @param id 元素 id
   * @param expectedClass 期望的元素类型
   * @param <T> 期望元素类型
   * @return 对应元素，不存在或类型不符时抛出 {@link BpmnParseException}
   */
  public <T extends BpmnFlowElement> T getElementById(
      final String id, final Class<T> expectedClass) {
    BpmnFlowElement element = flowElements.get(id);
    if (element == null) {
      throw new BpmnParseException("No element with id '" + id + "' found in process");
    }
    if (element instanceof final BpmnMultiInstanceBody multiInstanceBody
        && !BpmnMultiInstanceBody.class.isAssignableFrom(expectedClass)) {
      element = multiInstanceBody.getInnerActivity();
    }
    if (!expectedClass.isAssignableFrom(element.getClass())) {
      throw new BpmnParseException(
          String.format(
              "Element with id '%s' is of type '%s' but expected '%s'",
              id, expectedClass.getSimpleName(), element.getClass().getSimpleName()));
    }
    return expectedClass.cast(element);
  }

  /** 获取全部流程元素（含流程自身）。 */
  public Collection<BpmnFlowElement> getFlowElements() {
    return flowElements.values();
  }

  /** 获取历史数据存活时长（天），未声明时返回 null。 */
  public Integer getHistoryTimeToLive() {
    return historyTimeToLive;
  }

  /**
   * 设置历史数据存活时长。
   *
   * @param historyTimeToLive 存活时长（天）
   */
  public void setHistoryTimeToLive(final int historyTimeToLive) {
    this.historyTimeToLive = historyTimeToLive;
  }

  /** 获取历史数据存活时长原始表达式，未声明时返回 null。 */
  public ScriptExpression getHistoryTimeToLiveExpression() {
    return historyTimeToLiveExpression;
  }

  /**
   * 设置历史数据存活时长原始表达式。
   *
   * @param historyTimeToLiveExpression 存活时长表达式
   */
  public void setHistoryTimeToLiveExpression(final ScriptExpression historyTimeToLiveExpression) {
    this.historyTimeToLiveExpression = historyTimeToLiveExpression;
  }

  /** 是否可执行流程。 */
  public boolean isExecutable() {
    return executable;
  }

  /**
   * 设置是否可执行流程。
   *
   * @param executable 是否可执行
   */
  public void setExecutable(final boolean executable) {
    this.executable = executable;
  }
}
