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
package com.anyilanxin.kunpeng.engine.script.impl;

import com.anyilanxin.kunpeng.engine.script.Language;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.engine.script.ScriptLanguage;
import com.anyilanxin.kunpeng.engine.script.impl.javascript.JavaScriptLanguage;
import com.anyilanxin.kunpeng.engine.script.impl.python.PythonLanguage;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QlExpressLanguage;
import javax.script.ScriptEngineManager;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author zxuanhong
 * @date 2025-11-10 11:56
 * @since
 */
public final class DefaultScriptEngine implements ScriptEngine {
  private final ScriptLanguage[] scriptLanguages;

  private DefaultScriptEngine(final BeanFactory beanFactory) {
    if (SingletonHolder.INSTANCE != null) {
      throw new RuntimeException("禁止反射");
    }
    final ScriptEngineManager engineManager = new ScriptEngineManager();
    scriptLanguages = new ScriptLanguage[Language.values().length + 1];
    for (final Language language : Language.values()) {
      switch (language) {
        case DEFAULT, QL_EXPRESS ->
            scriptLanguages[language.ordinal()] = new QlExpressLanguage(beanFactory);
        case JAVA_SCRIPT ->
            scriptLanguages[language.ordinal()] = new JavaScriptLanguage(engineManager);
        case PYTHON -> scriptLanguages[language.ordinal()] = new PythonLanguage(engineManager);
      }
    }
  }

  private static class SingletonHolder {
    private static volatile ScriptEngine INSTANCE;

    static void initialize(final BeanFactory beanFactory) {
      if (INSTANCE == null) {
        INSTANCE = new DefaultScriptEngine(beanFactory);
      }
    }
  }

  public static ScriptEngine getInstance(final BeanFactory beanFactory) {
    if (SingletonHolder.INSTANCE == null) {
      synchronized (SingletonHolder.class) {
        if (SingletonHolder.INSTANCE == null) {
          SingletonHolder.initialize(beanFactory);
        }
      }
    }
    return SingletonHolder.INSTANCE;
  }

  @Override
  public ScriptExpression parse(final String scriptExpression) {
    return parse(scriptExpression, Language.DEFAULT);
  }

  @Override
  public ScriptExpression parse(final String scriptExpression, final Language language) {
    final StaticExpression staticExpression =
        new StaticExpression(scriptExpression, Language.DEFAULT);
    if (staticExpression.isStatic()) {
      return staticExpression;
    }
    return scriptLanguages[language.ordinal()].parse(Strings.CS.removeStart(scriptExpression, "="));
  }
}
