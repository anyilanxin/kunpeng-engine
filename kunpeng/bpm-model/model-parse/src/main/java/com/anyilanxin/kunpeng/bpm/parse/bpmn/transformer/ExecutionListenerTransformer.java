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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListener;
import com.anyilanxin.kunpeng.bpm.parse.BpmParseLogger;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowElement;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import java.util.Collection;
import org.slf4j.Logger;

/**
 * 执行监听器转换器：把 kunpeng:executionListeners 声明装配到宿主元素（流程、节点或顺序流）。
 *
 * <p>声明缺失必要属性（eventType/type/retries）或解析异常时记录日志并跳过该条监听器，不中断整体转换。
 */
final class ExecutionListenerTransformer {
  private static final Logger LOGGER = BpmParseLogger.PARSE_BPMN_LOGGER;

  private ExecutionListenerTransformer() {}

  /**
   * 批量装配执行监听器到宿主元素。
   *
   * @param host 监听器宿主（流程、节点或顺序流）
   * @param executionListeners 监听器声明集合
   * @param context 转换上下文
   */
  static void addListeners(
      final BpmnFlowElement host,
      final Collection<KunpengExecutionListener> executionListeners,
      final BpmnTransformContext context) {
    if (executionListeners == null || executionListeners.isEmpty()) {
      return;
    }
    for (final KunpengExecutionListener listener : executionListeners) {
      try {
        if (listener.getEventType() == null
            || listener.getType() == null
            || listener.getRetries() == null) {
          LOGGER.debug(
              "Incomplete execution listener declaration on element '{}' (eventType/type/retries"
                  + " are all required), listener definition ignored",
              host.getId());
          continue;
        }
        host.addExecutionListener(
            listener.getEventType(),
            context.parseExpression(listener.getType()),
            context.parseExpression(listener.getRetries()));
      } catch (final Exception e) {
        // 防御性兜底：单条监听器异常不应中断整体转换（避免回放时阻断分区）
        LOGGER.warn(
            "Failed to transform execution listener on element '{}' ({}: {}), listener"
                + " definition ignored",
            host.getId(),
            e.getClass().getSimpleName(),
            e.getMessage());
      }
    }
  }
}
