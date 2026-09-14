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

/**
 * "-"（任意匹配）一元测试转换器：FEEL 中一元测试为 "-" 时表示匹配任意输入值。
 *
 * @author zxuanhong
 * @since
 */
public class HyphenTransform implements QlexpressTransformer {
  /** 返回该转换器适配的值类型：hyphen。 */
  @Override
  public DmnValueType valueType() {
    return DmnValueType.HYPHEN;
  }

  /**
   * 任意匹配始终成立，无需实际判断。
   *
   * @param simpleUnaryTests 一元测试表达式原文
   * @param inputName 决策表输入变量名
   * @return 字面量 true
   */
  @Override
  public String transformSimpleUnaryTests(final String simpleUnaryTests, final String inputName) {
    return "true";
  }
}
