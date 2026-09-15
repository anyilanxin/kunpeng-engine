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

import java.util.HashMap;
import java.util.Map;

/**
 * 流程元素的运行时类型标识。
 *
 * <p>每个枚举值携带其对应的 BPMN XSD 元素名；{@link #forTypeName(String)} 依据元素名（如 {@code serviceTask}、{@code
 * startEvent}）完成查找，未知类型统一归入 {@link #UNSPECIFIED}。
 */
public enum BpmnElementType {
  /** 未指定类型（初始态或无法识别的元素） */
  UNSPECIFIED(""),
  /** 流程定义（process） */
  PROCESS("process"),
  /** 开始事件 */
  START_EVENT("startEvent"),
  /** 结束事件 */
  END_EVENT("endEvent"),
  /** 中间捕获事件 */
  INTERMEDIATE_CATCH_EVENT("intermediateCatchEvent"),
  /** 中间抛出事件 */
  INTERMEDIATE_THROW_EVENT("intermediateThrowEvent"),
  /** 边界事件 */
  BOUNDARY_EVENT("boundaryEvent"),
  /** 事件子流程（triggeredByEvent 的 subProcess） */
  EVENT_SUB_PROCESS("eventSubProcess"),
  /** 内嵌子流程 */
  SUB_PROCESS("subProcess"),
  /** 顺序流 */
  SEQUENCE_FLOW("sequenceFlow"),
  /** 并行网关 */
  PARALLEL_GATEWAY("parallelGateway"),
  /** 排他网关 */
  EXCLUSIVE_GATEWAY("exclusiveGateway"),
  /** 包容网关 */
  INCLUSIVE_GATEWAY("inclusiveGateway"),
  /** 事件网关 */
  EVENT_BASED_GATEWAY("eventBasedGateway"),
  /** 复杂网关 */
  COMPLEX_GATEWAY("complexGateway"),
  /** 服务任务 */
  SERVICE_TASK("serviceTask"),
  /** 接收任务 */
  RECEIVE_TASK("receiveTask"),
  /** 用户任务 */
  USER_TASK("userTask"),
  /** 发送任务 */
  SEND_TASK("sendTask"),
  /** 手工任务 */
  MANUAL_TASK("manualTask"),
  /** 业务规则任务 */
  BUSINESS_RULE_TASK("businessRuleTask"),
  /** 脚本任务 */
  SCRIPT_TASK("scriptTask"),
  /** 调用活动 */
  CALL_ACTIVITY("callActivity"),
  /** 一般任务（task） */
  TASK("task"),
  /** 临时子流程 */
  AD_HOC_SUB_PROCESS("adHocSubProcess"),
  /** 多实例活动体（由转换器构造，不对应具体 XSD 元素名） */
  MULTI_INSTANCE_BODY("multiInstanceBody");

  /** XSD 元素名 -> 枚举的索引，静态构建一次，避免每次查找线性扫描 */
  private static final Map<String, BpmnElementType> TYPE_NAME_INDEX;

  static {
    final BpmnElementType[] constants = values();
    TYPE_NAME_INDEX = new HashMap<>(constants.length * 2);
    for (final BpmnElementType type : constants) {
      // 空名与 eventSubProcess 不参与按名查找：subProcess 由转换器按 triggeredByEvent 覆写类型
      if (!type.typeName.isEmpty() && type != EVENT_SUB_PROCESS) {
        TYPE_NAME_INDEX.put(type.typeName, type);
      }
    }
  }

  /** 本类型对应的 BPMN XSD 元素名 */
  private final String typeName;

  BpmnElementType(final String typeName) {
    this.typeName = typeName;
  }

  /**
   * 依据 BPMN XSD 元素名解析元素类型。
   *
   * @param typeName XSD 元素名（如 {@code serviceTask}）
   * @return 对应的元素类型，无法识别时返回 {@link #UNSPECIFIED}
   */
  public static BpmnElementType forTypeName(final String typeName) {
    return TYPE_NAME_INDEX.getOrDefault(typeName, UNSPECIFIED);
  }

  /**
   * 本枚举对应的 XSD 元素名。
   *
   * @return XSD 元素名，{@link #UNSPECIFIED} 返回空串
   */
  public String typeName() {
    return typeName;
  }
}
