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
package com.anyilanxin.kunpeng.bpm.parse.bpmn;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformer;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import com.anyilanxin.kunpeng.engine.script.ScriptEngineFactory;
import org.springframework.beans.factory.BeanFactory;

/** BPMN 解析组件工厂：创建模型转换器与模型校验器。 */
public final class BpmnFactory {

  private BpmnFactory() {}

  /**
   * 基于 Spring {@link BeanFactory} 创建 BPMN 模型转换器，内部会先创建表达式引擎再注入转换器。
   *
   * @param beanFactory Spring Bean 工厂
   * @return BPMN 模型转换器
   */
  public static BpmnTransformer createTransformer(final BeanFactory beanFactory) {
    return new BpmnTransformer(createExpressionLanguage(beanFactory));
  }

  /**
   * 使用指定的表达式引擎创建 BPMN 模型转换器。
   *
   * @param scriptEngine 表达式引擎
   * @return BPMN 模型转换器
   */
  public static BpmnTransformer createTransformer(final ScriptEngine scriptEngine) {
    return new BpmnTransformer(scriptEngine);
  }

  /**
   * 基于 Spring {@link BeanFactory} 创建 BPMN 模型校验器。
   *
   * @param beanFactory Spring Bean 工厂
   * @param maxCollectedResults 校验结果输出的最大条数（超出部分只计数）
   * @return BPMN 模型校验器
   */
  public static BpmnValidator createValidator(
      final BeanFactory beanFactory, final int maxCollectedResults) {
    return new BpmnValidator(maxCollectedResults);
  }

  /**
   * 基于 Spring {@link BeanFactory} 创建表达式引擎。
   *
   * @param beanFactory Spring Bean 工厂
   * @return 表达式引擎
   */
  public static ScriptEngine createExpressionLanguage(final BeanFactory beanFactory) {
    return ScriptEngineFactory.createScriptEngine(beanFactory);
  }
}
