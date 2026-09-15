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
package com.anyilanxin.kunpeng.bpm.parse.dmn.expression.impl;

import com.anyilanxin.kunpeng.bpm.parse.dmn.TransformUtil;
import com.anyilanxin.kunpeng.bpm.parse.dmn.expression.DmnValueType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.expression.ExpressTransformer;

/**
 * 日期类型（date）的一元测试转换器：区间语法转换为 QlExpress 的 between 判断，其余直接与输入变量拼接。
 *
 * @author zxuanhong
 * @since
 */
public class DateTransform implements ExpressTransformer {

  /** 返回该转换器适配的值类型：date。 */
  @Override
  public DmnValueType valueType() {
    return DmnValueType.DATE;
  }

  /**
   * 将日期一元测试转换为 QlExpress 表达式：匹配区间语法时转换为 between 区间判断，否则原样拼接为比较表达式。
   *
   * @param simpleUnaryTests 一元测试表达式原文
   * @param inputName 决策表输入变量名
   * @return 转换后的 QlExpress 表达式
   */
  @Override
  public String transformSimpleUnaryTests(final String simpleUnaryTests, final String inputName) {
    final String expression = TransformUtil.rangeType(simpleUnaryTests);
    if (expression != null) {
      return "%s between %s".formatted(inputName, expression);
    }
    return "%s %s".formatted(inputName, simpleUnaryTests);
  }
}
