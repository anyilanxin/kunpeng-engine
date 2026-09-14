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
package com.anyilanxin.kunpeng.bpm.parse.dmn.util;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.qladapter.RangeType;
import java.util.regex.Matcher;

/**
 * FEEL 表达式转换辅助工具类，提供 SimpleUnaryTests 区间表达式的格式化与表达式非空判断等静态方法。
 *
 * @author zxuanhong
 * @since
 */
public class TransformUtil {

  private TransformUtil() {}

  /**
   * 将 FEEL SimpleUnaryTests 区间表达式转换为对应的区间字符串。
   *
   * <p>遍历 {@link RangeType} 的所有匹配模式，命中后取捕获的左右端点值进行格式化。
   *
   * @param simpleUnaryTests SimpleUnaryTests 区间表达式
   * @return 转换后的区间表达式，无匹配模式时返回 null
   */
  public static String rangeType(final String simpleUnaryTests) {
    for (final RangeType type : RangeType.values()) {
      final Matcher matcher = type.pattern().matcher(simpleUnaryTests);
      if (matcher.matches()) {
        final String leftValue = matcher.group(1);
        final String rightValue = matcher.group(2);
        return type.format(leftValue, rightValue);
      }
    }
    return null;
  }

  /**
   * 判断表达式对象存在且表达式内容非空白。
   *
   * @param expression 表达式对象
   * @return 表达式对象及其内容均存在且非空白时返回 true
   */
  public static boolean isNonEmptyExpression(final DmnExpressionImpl expression) {
    return expression != null
        && expression.getExpression() != null
        && !expression.getExpression().trim().isEmpty();
  }
}
