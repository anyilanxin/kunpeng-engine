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
package com.anyilanxin.kunpeng.bpm.parse.dmn;

import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.DmnTransformer;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import com.anyilanxin.kunpeng.engine.script.ScriptEngineFactory;
import org.springframework.beans.factory.BeanFactory;

/**
 * DMN 解析模块的工厂类，用于创建 DMN 模型转换器 {@link DmnTransformer} 及其依赖的表达式语言引擎 {@link ScriptEngine}。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class DmnFactory {
  /**
   * 基于Spring {@link BeanFactory} 创建 DMN 模型转换器，内部会先创建表达式语言引擎再注入转换器。
   *
   * @param beanFactory Spring Bean 工厂
   * @return DMN 模型转换器
   */
  public static DmnTransformer createTransformer(final BeanFactory beanFactory) {
    return new DmnTransformer(createExpressionLanguage(beanFactory));
  }

  /**
   * 使用指定的表达式语言引擎创建 DMN 模型转换器。
   *
   * @param scriptEngine 表达式语言引擎
   * @return DMN 模型转换器
   */
  public static DmnTransformer createTransformer(final ScriptEngine scriptEngine) {
    return new DmnTransformer(scriptEngine);
  }

  /**
   * 基于Spring {@link BeanFactory} 创建表达式语言引擎。
   *
   * @param beanFactory Spring Bean 工厂
   * @return 表达式语言引擎
   */
  public static ScriptEngine createExpressionLanguage(final BeanFactory beanFactory) {
    return ScriptEngineFactory.createScriptEngine(beanFactory);
  }
}
