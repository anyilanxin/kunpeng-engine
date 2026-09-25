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
package com.anyilanxin.kunpeng.bpm.parse.dmn.expression;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableInputImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.expression.impl.*;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link SimpleUnaryTestsTransform} 的默认实现，按决策表输入的 DMN 值类型把一元测试分发给对应的 {@link ExpressTransformer} 完成
 * QlExpress 适配。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DefaultSimpleUnaryTestsTransform implements SimpleUnaryTestsTransform {
  private final Map<DmnValueType, ExpressTransformer> transform = new HashMap<>();

  /** 构造转换器并注册各 DMN 值类型的转换实现。 */
  public DefaultSimpleUnaryTestsTransform() {
    init();
  }

  private void init() {
    // DMN 标准数值 typeRef（integer/long/double）与 number 共用同一数值转换
    final NumberTransform numberTransform = new NumberTransform();
    transform.put(DmnValueType.NUMBER, numberTransform);
    transform.put(DmnValueType.INTEGER, numberTransform);
    transform.put(DmnValueType.LONG, numberTransform);
    transform.put(DmnValueType.DOUBLE, numberTransform);
    register(new AnyTransform())
        .register(new BooleanTransform())
        .register(new DatetimeDurationTransform())
        .register(new DatetimeTransform())
        .register(new TimeTransform())
        .register(new DateTransform())
        .register(new HyphenTransform())
        .register(new StringTransform())
        .register(new YearMonthDurationTransform());
  }

  private DefaultSimpleUnaryTestsTransform register(final ExpressTransformer transform) {
    this.transform.put(transform.valueType(), transform);
    return this;
  }

  /**
   * 按输入列的 DMN 值类型分发到对应转换器完成转换；类型未识别而输入变量为 "-" 时按任意匹配处理，其余情况原样返回表达式。
   *
   * @param dmnExpression 决策表条目的表达式元素
   * @param input 决策表输入列
   * @return 转换后的 QlExpress 表达式；无法识别类型时返回原表达式
   */
  @Override
  public String transformSimpleUnaryTests(
      final DmnExpressionImpl dmnExpression, final DmnDecisionTableInputImpl input) {
    final DmnValueType dmnValueType =
        DmnValueType.fromString(input.getExpression().getTypeDefinition().getTypeName());
    if (dmnValueType != null) {
      final ExpressTransformer testTransform = transform.get(dmnValueType);
      return testTransform.transformSimpleUnaryTests(
          dmnExpression.getExpression(), input.getInputVariable());
    } else if ("-".equals(input.getInputVariable())) {
      final ExpressTransformer testTransform = transform.get(DmnValueType.HYPHEN);
      return testTransform.transformSimpleUnaryTests(
          dmnExpression.getExpression(), input.getInputVariable());
    }
    return dmnExpression.getExpression();
  }
}
