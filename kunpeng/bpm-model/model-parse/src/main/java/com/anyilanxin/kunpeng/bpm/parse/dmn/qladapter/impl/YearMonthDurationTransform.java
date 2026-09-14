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
package com.anyilanxin.kunpeng.bpm.parse.dmn.qladapter.impl;

import com.anyilanxin.kunpeng.bpm.parse.dmn.qladapter.DmnValueType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.qladapter.QlexpressTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.util.TransformUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 年-月持续时间类型（yearMonthDuration）的一元测试转换器：区间语法转 between，比较运算符开头转比较表达式，其余转相等判断。
 *
 * @author zxuanhong
 * @since
 */
public class YearMonthDurationTransform implements QlexpressTransformer {
  private static final Pattern COMPARISON_PATTERN = Pattern.compile("^(==|>=|<=|>|<).+$");

  /** 返回该转换器适配的值类型：yearMonthDuration。 */
  @Override
  public DmnValueType valueType() {
    return DmnValueType.YEAR_MONTH;
  }

  /**
   * 将年-月持续时间一元测试转换为 QlExpress 表达式：优先匹配区间语法转 between；否则以比较运算符（==、&gt;=、&lt;=、
   * &gt;、&lt;）开头时拼接为比较表达式，最后退化为相等判断。
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
    } else {
      final Matcher matcher = COMPARISON_PATTERN.matcher(simpleUnaryTests);
      if (matcher.find()) {
        return "%s %s".formatted(inputName, simpleUnaryTests);
      } else {
        return "%s == %s".formatted(inputName, simpleUnaryTests);
      }
    }
  }
}
