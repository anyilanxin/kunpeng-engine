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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengExecutionListeners;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.utils.Either;

/** 流程定义转换器：创建流程运行时元素并解析历史数据存活时长与流程级执行监听器。 */
public final class ProcessTransformer implements ElementTransformer<Process> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<Process> getType() {
    return Process.class;
  }

  /**
   * 创建流程运行时元素、登记到上下文并设为当前流程；随后解析可选的历史存活时长与流程级监听器。
   *
   * @param element 流程模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final Process element, final BpmnTransformContext context) {
    final BpmnProcess process = new BpmnProcess(element.getId());
    process.setName(element.getName());
    process.setExecutable(element.isExecutable());
    context.addProcess(process);
    context.setCurrentProcess(process);
    transformHistoryTimeToLive(element, process, context);
    final KunpengExecutionListeners listeners =
        element.getSingleExtensionElement(KunpengExecutionListeners.class);
    if (listeners != null) {
      ExecutionListenerTransformer.addListeners(
          process, listeners.getExecutionListeners(), context);
    }
  }

  /** 解析历史数据存活时长：求值静态表达式并校验值域（不小于 -1）。 */
  private void transformHistoryTimeToLive(
      final Process element, final BpmnProcess process, final BpmnTransformContext context) {
    final String historyTimeToLiveString = element.getHistoryTimeToLiveString();
    if (historyTimeToLiveString == null) {
      return;
    }
    final ScriptExpression expression = context.parseExpression(historyTimeToLiveString);
    process.setHistoryTimeToLiveExpression(expression);
    final Either<String, Number> result = expression.evaluateNumber();
    if (result.isRight()) {
      final int historyTimeToLive = result.get().intValue();
      if (historyTimeToLive < -1) {
        throw new IllegalArgumentException(
            "Invalid history time to live, must be >= -1: " + historyTimeToLiveString);
      }
      process.setHistoryTimeToLive(historyTimeToLive);
    }
  }
}
