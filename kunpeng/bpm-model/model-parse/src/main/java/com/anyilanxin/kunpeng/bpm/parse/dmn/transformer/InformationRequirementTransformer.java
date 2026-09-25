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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.bpm.parse.dmn.transformer;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Decision;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InformationRequirement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecision;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.DmnDecisionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.ModelElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformContext;

/**
 * 信息依赖装配：把 {@code <informationRequirement>} 的 {@code requiredDecision} 引用 解析为已转换的 {@link
 * DmnDecision} 并挂到发起依赖的决策上（{@link DmnDecisionImpl#addRequiredDecision}）—— 求值侧（多层决策）按
 * requiredDecisions 逐层先求值子决策。
 *
 * <p>本转换器运行在决策注册（step4）之后，引用目标无论在文档中的先后顺序均可解析。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class InformationRequirementTransformer
    implements ModelElementTransformer<InformationRequirement> {
  /** 返回本转换器处理的 DMN 模型元素类型。 */
  @Override
  public Class<InformationRequirement> getType() {
    return InformationRequirement.class;
  }

  /**
   * 解析信息依赖：将被引用的决策挂到发起依赖的决策上。
   *
   * <p>requiredDecision 缺失或引用目标未注册时跳过装配。
   *
   * @param element 待转换的 InformationRequirement 模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final InformationRequirement element, final TransformContext context) {
    final Decision requiredDecisionElement = element.getRequiredDecision();
    if (requiredDecisionElement == null) {
      return;
    }
    final DmnDecision required =
        context.getElement(ElementType.DECISION, requiredDecisionElement.getId());
    final Decision requiringElement = requiringDecisionOf(element);
    if (requiringElement == null) {
      return;
    }
    final DmnDecisionImpl requiring =
        context.getElement(ElementType.DECISION, requiringElement.getId());
    if (required != null && requiring != null) {
      requiring.addRequiredDecision(required);
    }
  }

  /** 向上查找发起依赖的决策元素（informationRequirement 的祖先 Decision）。 */
  private Decision requiringDecisionOf(final InformationRequirement element) {
    ModelElementInstance current = element.getParentElement();
    while (current != null && !(current instanceof Decision)) {
      current = current.getParentElement();
    }
    return (Decision) current;
  }
}
