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

/** DMN 值类型的一元测试转换器接口，将 FEEL simple unary tests 表达式适配为 QlExpress 可执行表达式。 */
public interface ExpressTransformer {
  /** 返回该转换器适配的 DMN 值类型。 */
  DmnValueType valueType();

  /**
   * 将 FEEL simple unary tests 表达式转换为 QlExpress 表达式。
   *
   * @param simpleUnaryTests 一元测试表达式原文
   * @param inputName 决策表输入变量名
   * @return 转换后的 QlExpress 表达式
   */
  String transformSimpleUnaryTests(String simpleUnaryTests, String inputName);
}
