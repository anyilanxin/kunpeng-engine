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
package com.anyilanxin.kunpeng.bpm.parse.dmn;

import static com.anyilanxin.kunpeng.bpm.parse.dmn.DmnFactory.createExpressionLanguage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecision;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecisionRequirementsGraph;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.HitPolicyType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisionliteral.DmnDecisionLiteralExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableInputImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableRuleImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.DmnTransformer;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/**
 * DMN 模型转换测试（自 dmn-engine 迁入，改走 {@link DmnTransformer} 新 API）： 决策表结构（输入/输出/规则/命中策略）、
 * 表达式脚本装配、字面表达式决策与决策依赖（DRG）装配。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class DmnTransformTest {

  /** 单输入单输出单规则表（输入变量 input，输出字面量 "ok"）。 */
  private static final String ONE_RULE_DMN = "dmn/OneRule.dmn";

  /** 零输入单输出表（常量决策表——校验输入/输出数量解耦）。 */
  private static final String NO_INPUT_DMN = "dmn/NoInput.dmn";

  /** 双输入双输出四规则表（含标准数值 typeRef=double、多值与比较单目测试）。 */
  private static final String EXAMPLE_DMN = "dmn/Example.dmn";

  /** 字面表达式决策 + 决策表依赖（informationRequirement）。 */
  private static final String DRG_EXAMPLE_DMN = "dmn/DrgExample.dmn";

  @Test
  void shouldTransformSimpleDecisionTable() {
    final DmnDecision decision = transform(ONE_RULE_DMN).getDecision("decision");

    assertNotNull(decision);
    assertEquals("decision", decision.getKey());
    assertTrue(decision.isDecisionTable());

    final var table = (DmnDecisionTableImpl) decision.getDecisionLogic();
    assertEquals(1, table.getInputs().size());
    // 输入表达式文本即取值来源；绑定名缺省为 cellInput（kunpeng:inputVariable 扩展属性可覆盖）
    assertEquals("input", table.getInputs().get(0).getExpression().getExpression());
    assertEquals(
        DmnDecisionTableInputImpl.DEFAULT_INPUT_VARIABLE_NAME,
        table.getInputs().get(0).getInputVariable());
    assertEquals(1, table.getOutputs().size());
    assertEquals(1, table.getRules().size());
    assertEquals(HitPolicyType.UNIQUE, table.getHitPolicy());

    // 条件与结论都已装配可执行脚本表达式
    final DmnDecisionTableRuleImpl rule = table.getRules().get(0);
    assertParsableScript(rule.getConditions().get(0));
    assertParsableScript(rule.getConclusions().get(0));
    assertEquals("\"ok\"", rule.getConclusions().get(0).getExpression());
  }

  @Test
  void shouldTransformTableWithoutInputs() {
    final DmnDecision decision = transform(NO_INPUT_DMN).getDecision("decision");

    assertNotNull(decision);
    final var table = (DmnDecisionTableImpl) decision.getDecisionLogic();
    assertEquals(0, table.getInputs().size());
    assertEquals(1, table.getOutputs().size());
    // 无输入表仍有可执行输出
    assertParsableScript(table.getRules().get(0).getConclusions().get(0));
  }

  @Test
  void shouldTransformTableWithDifferentInputAndOutputCounts() {
    final DmnDecision decision = transform(EXAMPLE_DMN).getDecision("decision");

    assertNotNull(decision);
    final var table = (DmnDecisionTableImpl) decision.getDecisionLogic();
    assertEquals(2, table.getInputs().size());
    assertEquals("status", table.getInputs().get(0).getExpression().getExpression());
    assertEquals("sum", table.getInputs().get(1).getExpression().getExpression());
    assertEquals(2, table.getOutputs().size());
    assertEquals(4, table.getRules().size());

    // 每条规则的全部条件与结论均已装配脚本（含 double 输入的比较单目测试与空条件）
    for (final DmnDecisionTableRuleImpl rule : table.getRules()) {
      assertEquals(2, rule.getConditions().size());
      assertEquals(2, rule.getConclusions().size());
      rule.getConditions().forEach(DmnTransformTest::assertParsableScript);
      rule.getConclusions().forEach(DmnTransformTest::assertParsableScript);
    }
  }

  @Test
  void shouldTransformDrgWithLiteralExpressionAndRequiredDecision() {
    final DmnDecisionRequirementsGraph drg = transform(DRG_EXAMPLE_DMN);

    assertEquals(2, drg.getDecisions().size());
    assertNotNull(drg.getDecision("season-decision"));
    assertNotNull(drg.getDecision("dish-decision"));

    // 字面表达式决策：非决策表，表达式文本保留
    final DmnDecision seasonDecision = drg.getDecision("season-decision");
    assertFalse(seasonDecision.isDecisionTable());
    final var literal = (DmnDecisionLiteralExpressionImpl) seasonDecision.getDecisionLogic();
    assertEquals("\"summer\"", literal.getExpression().getExpression());

    // 决策依赖：dish-decision 依赖 season-decision，命中策略 FIRST
    final DmnDecision dishDecision = drg.getDecision("dish-decision");
    assertTrue(dishDecision.isDecisionTable());
    assertEquals(1, dishDecision.getRequiredDecisions().size());
    assertTrue(
        dishDecision.getRequiredDecisions().contains(seasonDecision),
        "Expected dish-decision to require season-decision");
    final var table = (DmnDecisionTableImpl) dishDecision.getDecisionLogic();
    assertEquals(HitPolicyType.FIRST, table.getHitPolicy());
  }

  private DmnDecisionRequirementsGraph transform(final String resource) {
    final DmnTransformer transformer =
        DmnFactory.createTransformer(createExpressionLanguage(null));
    try (final InputStream stream =
        getClass().getClassLoader().getResourceAsStream(resource)) {
      return transformer.transformDefinitions(stream.readAllBytes());
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to transform " + resource, e);
    }
  }

  private static void assertParsableScript(final DmnExpressionImpl expression) {
    assertNotNull(
        expression.getScriptExpression(),
        "Expected script expression to be set: " + expression.getExpression());
  }
}
