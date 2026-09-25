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

import com.anyilanxin.kunpeng.bpm.parse.dmn.expression.DmnValueType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.expression.ExpressTransformer;

/**
 * 布尔类型（boolean）的一元测试转换器，将一元测试转换为与输入变量的相等判断。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BooleanTransform implements ExpressTransformer {
  /** 返回该转换器适配的值类型：boolean。 */
  @Override
  public DmnValueType valueType() {
    return DmnValueType.BOOLEAN;
  }

  /**
   * 将布尔一元测试转换为 QlExpress 相等判断表达式。
   *
   * @param simpleUnaryTests 一元测试表达式原文
   * @param inputName 决策表输入变量名
   * @return 形如 {@code inputName == simpleUnaryTests} 的相等判断表达式
   */
  @Override
  public String transformSimpleUnaryTests(final String simpleUnaryTests, final String inputName) {
    return "%s == %s".formatted(inputName, simpleUnaryTests);
  }
}
