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
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.EncapsulatedLogic;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Variable;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecisionRequirementsGraphImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.businessknowledge.DmnBusinessKnowledgeFunctionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.businessknowledge.DmnBusinessKnowledgeImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.businessknowledge.DmnBusinessKnowledgeLogicImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnVariableImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.ModelElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformContext;

/**
 * 将 DMN BusinessKnowledgeModel（业务知识模型）转换为运行时 DmnBusinessKnowledge 元素：组装 Variable 信息项与
 * EncapsulatedLogic 决策逻辑，并注册到决策需求图。
 */
public final class BusinessKnowledgeModelTransformer
    implements ModelElementTransformer<BusinessKnowledgeModel> {
  /** 返回本转换器处理的 DMN 模型元素类型。 */
  @Override
  public Class<BusinessKnowledgeModel> getType() {
    return BusinessKnowledgeModel.class;
  }

  /**
   * 转换业务知识模型并注册到决策需求图。
   *
   * <p>从上下文取出已转换的 Variable 与 EncapsulatedLogic 组装业务知识逻辑；仅在存在封装逻辑时注册业务知识。
   *
   * @param element 待转换的 BusinessKnowledgeModel 模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final BusinessKnowledgeModel element, final TransformContext context) {
    final DmnBusinessKnowledgeImpl businessKnowledge = new DmnBusinessKnowledgeImpl();
    businessKnowledge.setName(element.getName());
    businessKnowledge.setKey(element.getId());
    boolean have = false;

    final DmnBusinessKnowledgeLogicImpl knowledgeLogic = new DmnBusinessKnowledgeLogicImpl();
    final Variable variable = element.getVariable();
    if (variable != null) {
      final DmnVariableImpl dmnVariable =
          context.getElement(ElementType.VARIABLE, variable.getId());
      if (dmnVariable != null) {
        knowledgeLogic.setVariable(dmnVariable);
      }
    }

    final EncapsulatedLogic encapsulatedLogic = element.getEncapsulatedLogic();
    if (encapsulatedLogic != null) {
      final DmnBusinessKnowledgeFunctionImpl knowledgeFunctionLogic =
          context.getElement(ElementType.ENCAPSULATED_LOGIC, encapsulatedLogic.getId());
      if (knowledgeFunctionLogic != null) {
        knowledgeLogic.setKnowledgeFunction(knowledgeFunctionLogic);
      }
      businessKnowledge.setBusinessKnowledgeLogic(knowledgeLogic);
      have = true;
    }

    final DmnDecisionRequirementsGraphImpl requirementsGraph = context.getRequirementsGraph();
    if (have) {
      requirementsGraph.addBusinessKnowledge(businessKnowledge);
      context.addBusinessKnowledge(businessKnowledge);
    }
  }
}
