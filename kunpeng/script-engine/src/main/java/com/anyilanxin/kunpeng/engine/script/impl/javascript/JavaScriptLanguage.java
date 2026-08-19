/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.engine.script.ScriptLanguage;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * java script 引擎
 *
 * @author zxuanhong
 * @date 2025-10-10 23:50
 * @since
 */
public final class JavaScriptLanguage implements ScriptLanguage {
  private final GraalJSScriptEngine engine;

  public JavaScriptLanguage(final ScriptEngineManager engineManager) {
    System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    engine =
        (GraalJSScriptEngine) engineManager.getEngineByName(Language.JAVA_SCRIPT.getShortName());
  }

  @Override
  public ScriptExpression parse(final String expression) {
    return new JavaScriptExpression(engine, expression);
  }
}
