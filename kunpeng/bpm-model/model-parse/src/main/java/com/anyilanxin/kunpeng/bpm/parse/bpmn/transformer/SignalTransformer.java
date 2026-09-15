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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Signal;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnSignal;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;
import com.anyilanxin.kunpeng.utils.Either;

/** 信号转换器：解析信号名表达式，静态信号名在部署期提前求值为常量。 */
public final class SignalTransformer implements ElementTransformer<Signal> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<Signal> getType() {
    return Signal.class;
  }

  /**
   * 创建信号运行时元素并注册到上下文（信号名缺失时不注册，由前置校验拦截）。
   *
   * @param element 信号模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final Signal element, final BpmnTransformContext context) {
    final String name = element.getName();
    if (name == null) {
      return;
    }
    final BpmnSignal signal = new BpmnSignal(element.getId());
    signal.setSignalNameExpression(context.parseExpression(name));
    if (signal.getSignalNameExpression().isStatic()) {
      final Either<String, String> result = signal.getSignalNameExpression().evaluateString();
      if (result.isRight()) {
        signal.setSignalName(result.get());
      }
    }
    context.addSignal(signal);
  }
}
