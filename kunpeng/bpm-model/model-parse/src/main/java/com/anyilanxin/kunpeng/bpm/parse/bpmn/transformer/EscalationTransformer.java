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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Escalation;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEscalation;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/** 升级转换器：解析升级码表达式，静态升级码在部署期提前求值为常量。 */
public final class EscalationTransformer implements ElementTransformer<Escalation> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<Escalation> getType() {
    return Escalation.class;
  }

  /**
   * 创建升级运行时元素并注册到上下文（未声明升级码时不解析表达式）。
   *
   * @param element 升级模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final Escalation element, final BpmnTransformContext context) {
    final BpmnEscalation escalation = new BpmnEscalation(element.getId());
    final String escalationCode = element.getEscalationCode();
    if (escalationCode != null) {
      escalation.setEscalationCodeExpression(context.parseExpression(escalationCode));
      if (escalation.getEscalationCodeExpression().isStatic()) {
        escalation.setEscalationCode(escalationCode);
      }
    }
    context.addEscalation(escalation);
  }
}
