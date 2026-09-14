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

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecisionRequirementsGraph;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformer.*;
import com.anyilanxin.kunpeng.bpm.model.dmn.Dmn;
import com.anyilanxin.kunpeng.bpm.model.dmn.DmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.dmn.traversal.ModelWalker;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;

/**
 * DMN 模型转换入口：将 DMN XML 模型分五个阶段（每阶段一次全量遍历）转换为运行时决策需求图（DRG）。
 *
 * <p>阶段顺序：基础表达式元素 → 决策表列与规则行 → 决策表与封装逻辑 → 决策与业务知识模型 → 信息/知识依赖装配。
 */
public final class DmnTransformer {
  /*
   * 步骤 1：实例化 dmn 业务知识中的所有元素
   */
  private final TransformationVisitor step1Visitor;

  /*
   * 步骤 1：实例化 dmn 决策中的所有元素
   */
  private final TransformationVisitor step2Visitor;

  /*
   * 步骤 1：实例化 dmn 决策中的所有元素
   */
  private final TransformationVisitor step3Visitor;
  /*
   * 步骤 4：装配 dmn 决策与业务知识模型
   */
  private final TransformationVisitor step4Visitor;
  /*
   * 步骤 5：装配 dmn 信息依赖与知识依赖
   */
  private final TransformationVisitor step5Visitor;
  /*
   * 表达式编译引擎（把 DMN 表达式编译为可执行脚本）
   */
  private final ScriptEngine expressionLanguage;

  /**
   * 构造转换器，并按依赖顺序为五个阶段各注册一组元素转换器。
   *
   * @param expressionLanguage 用于编译 DMN 表达式的脚本引擎
   */
  public DmnTransformer(final ScriptEngine expressionLanguage) {
    this.expressionLanguage = expressionLanguage;
    step1Visitor = new TransformationVisitor();
    step1Visitor.registerHandler(new VariableTransformer());
    step1Visitor.registerHandler(new LiteralExpressionTransformer());
    step1Visitor.registerHandler(new InputExpressionTransformer());
    step1Visitor.registerHandler(new InputEntryTransformer());
    step1Visitor.registerHandler(new OutputEntryTransformer());
    step1Visitor.registerHandler(new DefinitionsTransformer());

    step2Visitor = new TransformationVisitor();
    step2Visitor.registerHandler(new InputTransformer());
    step2Visitor.registerHandler(new OutputTransformer());
    step2Visitor.registerHandler(new RuleTransformer());

    step3Visitor = new TransformationVisitor();
    step3Visitor.registerHandler(new DecisionTableTransformer());
    step3Visitor.registerHandler(new EncapsulatedLogicTransformer());

    step4Visitor = new TransformationVisitor();
    step4Visitor.registerHandler(new BusinessKnowledgeModelTransformer());
    step4Visitor.registerHandler(new DecisionTransformer());

    step5Visitor = new TransformationVisitor();
    step5Visitor.registerHandler(new InformationRequirementTransformer());
    step5Visitor.registerHandler(new KnowledgeRequirementTransformer());
  }

  /**
   * 从 DMN XML 字节流读取模型并执行转换。
   *
   * @param bytes DMN XML 字节流
   * @return 转换得到的决策需求图
   */
  public DmnDecisionRequirementsGraph transformDefinitions(final byte[] bytes) {
    final DmnModelInstance dmnModelInstance = Dmn.readModelFromBytes(bytes);
    return transformDefinitions(dmnModelInstance);
  }

  /**
   * 分五个阶段遍历模型完成转换，并返回构建出的决策需求图。
   *
   * @param modelInstance 已解析的 DMN 模型实例
   * @return 转换得到的决策需求图
   */
  public DmnDecisionRequirementsGraph transformDefinitions(final DmnModelInstance modelInstance) {
    final TransformContext context = new TransformContext();
    context.setExpressionLanguage(expressionLanguage);

    final ModelWalker walker = new ModelWalker(modelInstance);
    step1Visitor.setContext(context);
    walker.walk(step1Visitor);

    step2Visitor.setContext(context);
    walker.walk(step2Visitor);

    step3Visitor.setContext(context);
    walker.walk(step3Visitor);

    step4Visitor.setContext(context);
    walker.walk(step4Visitor);

    step5Visitor.setContext(context);
    walker.walk(step5Visitor);

    return context.getRequirementsGraph();
  }
}
