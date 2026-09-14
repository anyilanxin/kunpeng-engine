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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串类型（string）的一元测试转换器：将一元测试转换为 QlExpress 的 in / not_in 集合判断。
 *
 * @author zxuanhong
 * @since
 */
public class StringTransform implements QlexpressTransformer {
  private static final Pattern NOT_PATTERN = Pattern.compile("^not\\((.*)\\)$");

  /** 返回该转换器适配的值类型：string。 */
  @Override
  public DmnValueType valueType() {
    return DmnValueType.STRING;
  }

  /**
   * 将字符串一元测试转换为 QlExpress 集合判断：{@code not(...)} 形式转换为 not_in，其余转换为 in。
   *
   * @param simpleUnaryTests 一元测试表达式原文
   * @param inputName 决策表输入变量名
   * @return 形如 {@code inputName in [...]} 或 {@code inputName not_in [...]} 的集合判断表达式
   */
  @Override
  public String transformSimpleUnaryTests(final String simpleUnaryTests, final String inputName) {
    final Matcher matcher = NOT_PATTERN.matcher(simpleUnaryTests);
    if (matcher.matches()) {
      return "%s not_in [%s]".formatted(inputName, matcher.group(1));
    } else {
      return "%s in [%s]".formatted(inputName, simpleUnaryTests);
    }
  }
}
