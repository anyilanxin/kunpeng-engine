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
package com.anyilanxin.kunpeng.bpm.parse.dmn.transformer;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.BusinessKnowledgeModel;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Decision;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.KnowledgeRequirement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnBusinessKnowledge;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.businessknowledge.DmnBusinessKnowledgeImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.DmnDecisionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.ModelElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformContext;

/**
 * 将 DMN KnowledgeRequirement（知识依赖）装配到运行时模型：按父元素类型把被引用的业务知识挂到发起依赖的 Decision 或 BusinessKnowledgeModel
 * 上。
 */
public final class KnowledgeRequirementTransformer
    implements ModelElementTransformer<KnowledgeRequirement> {
  /** 返回本转换器处理的 DMN 模型元素类型。 */
  @Override
  public Class<KnowledgeRequirement> getType() {
    return KnowledgeRequirement.class;
  }

  /**
   * 解析知识依赖：把被引用的业务知识挂到发起依赖的决策或业务知识模型上。
   *
   * <p>被引用的业务知识未注册时不做处理。
   *
   * @param element 待转换的 KnowledgeRequirement 模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final KnowledgeRequirement element, final TransformContext context) {
    final BusinessKnowledgeModel requiredKnowledge = element.getRequiredKnowledge();
    final String id = requiredKnowledge.getId();
    final ModelElementInstance parentElement = element.getParentElement();
    System.out.println(parentElement);

    final DmnBusinessKnowledge businessKnowledge = context.getBusinessKnowledge(id);
    if (businessKnowledge != null) {
      if (parentElement instanceof final Decision decision) {
        final DmnDecisionImpl dmnDecision = (DmnDecisionImpl) context.getDecision(decision.getId());
        if (dmnDecision != null) {
          dmnDecision.addRequiredBusinessKnowledge(businessKnowledge);
        }
      } else if (parentElement instanceof final BusinessKnowledgeModel businessKnowledgeModel) {
        final DmnBusinessKnowledgeImpl dmnBusinessKnowledge =
            (DmnBusinessKnowledgeImpl) context.getBusinessKnowledge(businessKnowledgeModel.getId());
        if (dmnBusinessKnowledge != null) {
          dmnBusinessKnowledge.addRequiredBusinessKnowledge(businessKnowledge);
        }
      }
    }
  }
}
