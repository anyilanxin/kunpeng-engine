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
package com.anyilanxin.kunpeng.engine.script.impl.javascript;

import com.anyilanxin.kunpeng.engine.script.Language;
import com.anyilanxin.kunpeng.engine.script.impl.jsr223.Jsr223Expression;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

/**
 * JavaScript 表达式实现：编译/求值复用 {@link Jsr223Expression} 基类（CompiledScript 构造期编译、求值期复用）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JavaScriptExpression extends Jsr223Expression {

  public JavaScriptExpression(final GraalJSScriptEngine engine, final String expression) {
    super(engine, expression);
  }

  @Override
  public Language getLanguage() {
    return Language.JAVA_SCRIPT;
  }
}
