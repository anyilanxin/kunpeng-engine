/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.engine.dmn.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngine;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngineConfiguration;
import com.anyilanxin.kunpeng.engine.dmn.delegate.DmnDecisionEvaluationListener;
import com.anyilanxin.kunpeng.engine.dmn.delegate.DmnDecisionTableEvaluationListener;
import com.anyilanxin.kunpeng.engine.dmn.impl.el.DefaultScriptEngineResolver;
import com.anyilanxin.kunpeng.engine.dmn.impl.el.JuelElProvider;
import com.anyilanxin.kunpeng.engine.dmn.impl.metrics.DefaultEngineMetricCollector;
import com.anyilanxin.kunpeng.engine.dmn.impl.metrics.DmnEngineMetricCollectorWrapper;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.el.DmnScriptEngineResolver;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.el.ElProvider;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnTransformer;
import com.anyilanxin.kunpeng.engine.dmn.impl.transform.DefaultDmnTransformer;
import com.anyilanxin.kunpeng.engine.dmn.spi.DmnEngineMetricCollector;
import org.camunda.bpm.dmn.feel.impl.FeelEngine;
import org.camunda.bpm.dmn.feel.impl.FeelEngineFactory;
import org.camunda.bpm.dmn.feel.impl.juel.FeelEngineFactoryImpl;
import org.camunda.bpm.dmn.feel.impl.scala.ScalaFeelEngineFactory;
import org.camunda.bpm.dmn.feel.impl.scala.function.FeelCustomFunctionProvider;
import com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants;

public class DefaultDmnEngineConfiguration extends DmnEngineConfiguration {

  public static final String FEEL_EXPRESSION_LANGUAGE = DmnModelConstants.FEEL_NS;
  public static final String FEEL_EXPRESSION_LANGUAGE_ALTERNATIVE = "feel";
  public static final String FEEL_EXPRESSION_LANGUAGE_DMN12 = DmnModelConstants.FEEL12_NS;
  public static final String FEEL_EXPRESSION_LANGUAGE_DMN13 = DmnModelConstants.FEEL13_NS;
  public static final String FEEL_EXPRESSION_LANGUAGE_DMN14 = DmnModelConstants.FEEL14_NS;
  public static final String FEEL_EXPRESSION_LANGUAGE_DMN15 = DmnModelConstants.FEEL15_NS;
  public static final String JUEL_EXPRESSION_LANGUAGE = "juel";

  protected DmnEngineMetricCollector engineMetricCollector;

  protected List<DmnDecisionTableEvaluationListener> customPreDecisionTableEvaluationListeners =
      new ArrayList<>();
  protected List<DmnDecisionTableEvaluationListener> customPostDecisionTableEvaluationListeners =
      new ArrayList<>();
  protected List<DmnDecisionTableEvaluationListener> decisionTableEvaluationListeners;

  // Decision evaluation listeners
  protected List<DmnDecisionEvaluationListener> decisionEvaluationListeners;
  protected List<DmnDecisionEvaluationListener> customPreDecisionEvaluationListeners =
      new ArrayList<>();
  protected List<DmnDecisionEvaluationListener> customPostDecisionEvaluationListeners =
      new ArrayList<>();

  protected DmnScriptEngineResolver scriptEngineResolver;
  protected ElProvider elProvider;
  protected FeelEngineFactory feelEngineFactory;
  protected FeelEngine feelEngine;

  /** a list of DMN FEEL custom function providers */
  protected List<FeelCustomFunctionProvider> feelCustomFunctionProviders;

  /** Enable FEEL legacy behavior */
  protected boolean enableFeelLegacyBehavior = false;

  protected String defaultInputExpressionExpressionLanguage = null;
  protected String defaultInputEntryExpressionLanguage = null;
  protected String defaultOutputEntryExpressionLanguage = null;
  protected String defaultLiteralExpressionLanguage = null;

  protected DmnTransformer transformer = new DefaultDmnTransformer();

  protected boolean returnBlankTableOutputAsNull = false;

  @Override
  public DmnEngine buildEngine() {
    init();
    return new DefaultDmnEngine(this);
  }

  public void init() {
    initMetricCollector();
    initDecisionTableEvaluationListener();
    initDecisionEvaluationListener();
    initScriptEngineResolver();
    initElDefaults();
    initElProvider();
    initFeelEngine();
  }

  public void initElDefaults() {
    if (enableFeelLegacyBehavior) {
      if (defaultInputExpressionExpressionLanguage == null) {
        defaultInputExpressionExpressionLanguage(JUEL_EXPRESSION_LANGUAGE);
      }
      if (defaultInputEntryExpressionLanguage == null) {
        defaultInputEntryExpressionLanguage(FEEL_EXPRESSION_LANGUAGE);
      }
      if (defaultOutputEntryExpressionLanguage == null) {
        defaultOutputEntryExpressionLanguage(JUEL_EXPRESSION_LANGUAGE);
      }
      if (defaultLiteralExpressionLanguage == null) {
        defaultLiteralExpressionLanguage(JUEL_EXPRESSION_LANGUAGE);
      }

    } else {
      if (defaultInputExpressionExpressionLanguage == null) {
        defaultInputExpressionExpressionLanguage(FEEL_EXPRESSION_LANGUAGE);
      }
      if (defaultInputEntryExpressionLanguage == null) {
        defaultInputEntryExpressionLanguage(FEEL_EXPRESSION_LANGUAGE);
      }
      if (defaultOutputEntryExpressionLanguage == null) {
        defaultOutputEntryExpressionLanguage(FEEL_EXPRESSION_LANGUAGE);
      }
      if (defaultLiteralExpressionLanguage == null) {
        defaultLiteralExpressionLanguage(FEEL_EXPRESSION_LANGUAGE);
      }
    }
  }

  protected void initMetricCollector() {
    if (engineMetricCollector == null) {
      engineMetricCollector = new DefaultEngineMetricCollector();
    }
  }

  protected void initDecisionTableEvaluationListener() {
    final List<DmnDecisionTableEvaluationListener> listeners = new ArrayList<>();
    if (customPreDecisionTableEvaluationListeners != null
        && !customPreDecisionTableEvaluationListeners.isEmpty()) {
      listeners.addAll(customPreDecisionTableEvaluationListeners);
    }

    if (customPostDecisionTableEvaluationListeners != null
        && !customPostDecisionTableEvaluationListeners.isEmpty()) {
      listeners.addAll(customPostDecisionTableEvaluationListeners);
    }
    decisionTableEvaluationListeners = listeners;
  }

  protected void initDecisionEvaluationListener() {
    final List<DmnDecisionEvaluationListener> listeners = new ArrayList<>();
    if (customPreDecisionEvaluationListeners != null
        && !customPreDecisionEvaluationListeners.isEmpty()) {
      listeners.addAll(customPreDecisionEvaluationListeners);
    }

    listeners.addAll(getDefaultDmnDecisionEvaluationListeners());

    if (customPostDecisionEvaluationListeners != null
        && !customPostDecisionEvaluationListeners.isEmpty()) {
      listeners.addAll(customPostDecisionEvaluationListeners);
    }
    decisionEvaluationListeners = listeners;
  }

  protected Collection<? extends DmnDecisionEvaluationListener>
      getDefaultDmnDecisionEvaluationListeners() {
    final List<DmnDecisionEvaluationListener> defaultListeners = new ArrayList<>();

    if (engineMetricCollector instanceof DmnDecisionEvaluationListener) {
      defaultListeners.add((DmnDecisionEvaluationListener) engineMetricCollector);
    } else {
      defaultListeners.add(new DmnEngineMetricCollectorWrapper(engineMetricCollector));
    }

    return defaultListeners;
  }

  protected void initElProvider() {
    if (elProvider == null) {
      elProvider = new JuelElProvider();
    }
  }

  protected void initScriptEngineResolver() {
    if (scriptEngineResolver == null) {
      scriptEngineResolver = new DefaultScriptEngineResolver();
    }
  }

  protected void initFeelEngine() {
    if (feelEngineFactory == null) {
      if (!enableFeelLegacyBehavior) {
        feelEngineFactory = new ScalaFeelEngineFactory(feelCustomFunctionProviders);

      } else {
        feelEngineFactory = new FeelEngineFactoryImpl();
      }
    }

    if (feelEngine == null) {
      feelEngine = feelEngineFactory.createInstance();
    }
  }

  @Override
  public DmnEngineMetricCollector getEngineMetricCollector() {
    return engineMetricCollector;
  }

  @Override
  public void setEngineMetricCollector(final DmnEngineMetricCollector engineMetricCollector) {
    this.engineMetricCollector = engineMetricCollector;
  }

  @Override
  public DefaultDmnEngineConfiguration engineMetricCollector(
    final DmnEngineMetricCollector engineMetricCollector) {
    setEngineMetricCollector(engineMetricCollector);
    return this;
  }

  @Override
  public List<DmnDecisionTableEvaluationListener> getCustomPreDecisionTableEvaluationListeners() {
    return customPreDecisionTableEvaluationListeners;
  }

  @Override
  public void setCustomPreDecisionTableEvaluationListeners(
    final List<DmnDecisionTableEvaluationListener> decisionTableEvaluationListeners) {
    customPreDecisionTableEvaluationListeners = decisionTableEvaluationListeners;
  }

  @Override
  public DefaultDmnEngineConfiguration customPreDecisionTableEvaluationListeners(
    final List<DmnDecisionTableEvaluationListener> decisionTableEvaluationListeners) {
    setCustomPreDecisionTableEvaluationListeners(decisionTableEvaluationListeners);
    return this;
  }

  @Override
  public List<DmnDecisionTableEvaluationListener> getCustomPostDecisionTableEvaluationListeners() {
    return customPostDecisionTableEvaluationListeners;
  }

  @Override
  public void setCustomPostDecisionTableEvaluationListeners(
    final List<DmnDecisionTableEvaluationListener> decisionTableEvaluationListeners) {
    customPostDecisionTableEvaluationListeners = decisionTableEvaluationListeners;
  }

  @Override
  public DefaultDmnEngineConfiguration customPostDecisionTableEvaluationListeners(
    final List<DmnDecisionTableEvaluationListener> decisionTableEvaluationListeners) {
    setCustomPostDecisionTableEvaluationListeners(decisionTableEvaluationListeners);
    return this;
  }

  @Override
  public List<DmnDecisionEvaluationListener> getCustomPreDecisionEvaluationListeners() {
    return customPreDecisionEvaluationListeners;
  }

  @Override
  public void setCustomPreDecisionEvaluationListeners(
    final List<DmnDecisionEvaluationListener> decisionEvaluationListeners) {
    customPreDecisionEvaluationListeners = decisionEvaluationListeners;
  }

  @Override
  public DefaultDmnEngineConfiguration customPreDecisionEvaluationListeners(
    final List<DmnDecisionEvaluationListener> decisionEvaluationListeners) {
    setCustomPreDecisionEvaluationListeners(decisionEvaluationListeners);
    return this;
  }

  @Override
  public List<DmnDecisionEvaluationListener> getCustomPostDecisionEvaluationListeners() {
    return customPostDecisionEvaluationListeners;
  }

  @Override
  public void setCustomPostDecisionEvaluationListeners(
    final List<DmnDecisionEvaluationListener> decisionEvaluationListeners) {
    customPostDecisionEvaluationListeners = decisionEvaluationListeners;
  }

  @Override
  public DefaultDmnEngineConfiguration customPostDecisionEvaluationListeners(
    final List<DmnDecisionEvaluationListener> decisionEvaluationListeners) {
    setCustomPostDecisionEvaluationListeners(decisionEvaluationListeners);
    return this;
  }

  /**
   * The list of decision table evaluation listeners of the configuration. Contains the pre, default
   * and post decision table evaluation listeners. Is set during the build of an engine.
   *
   * @return the list of decision table evaluation listeners
   */
  public List<DmnDecisionTableEvaluationListener> getDecisionTableEvaluationListeners() {
    return decisionTableEvaluationListeners;
  }

  /**
   * The list of decision evaluation listeners of the configuration. Contains the pre, default and
   * post decision evaluation listeners. Is set during the build of an engine.
   *
   * @return the list of decision table evaluation listeners
   */
  public List<DmnDecisionEvaluationListener> getDecisionEvaluationListeners() {
    return decisionEvaluationListeners;
  }

  /**
   * @return the script engine resolver
   */
  public DmnScriptEngineResolver getScriptEngineResolver() {
    return scriptEngineResolver;
  }

  /**
   * Set the script engine resolver which is used by the engine to get an instance of a script
   * engine to evaluated expressions.
   *
   * @param scriptEngineResolver the script engine resolver
   */
  public void setScriptEngineResolver(final DmnScriptEngineResolver scriptEngineResolver) {
    this.scriptEngineResolver = scriptEngineResolver;
  }

  /**
   * Set the script engine resolver which is used by the engine to get an instance of a script
   * engine to evaluated expressions.
   *
   * @param scriptEngineResolver the script engine resolver
   * @return this
   */
  public DefaultDmnEngineConfiguration scriptEngineResolver(
    final DmnScriptEngineResolver scriptEngineResolver) {
    setScriptEngineResolver(scriptEngineResolver);
    return this;
  }

  /**
   * @return the el provider
   */
  public ElProvider getElProvider() {
    return elProvider;
  }

  /**
   * Set the el provider which is used by the engine to evaluate an el expression.
   *
   * @param elProvider the el provider
   */
  public void setElProvider(final ElProvider elProvider) {
    this.elProvider = elProvider;
  }

  /**
   * Set the el provider which is used by the engine to evaluate an el expression.
   *
   * @param elProvider the el provider
   * @return this
   */
  public DefaultDmnEngineConfiguration elProvider(final ElProvider elProvider) {
    setElProvider(elProvider);
    return this;
  }

  /**
   * @return the factory is used to create a {@link FeelEngine}
   */
  public FeelEngineFactory getFeelEngineFactory() {
    return feelEngineFactory;
  }

  /**
   * Set the factory to create a {@link FeelEngine}
   *
   * @param feelEngineFactory the feel engine factory
   */
  public void setFeelEngineFactory(final FeelEngineFactory feelEngineFactory) {
    this.feelEngineFactory = feelEngineFactory;
    feelEngine = null; // clear cached FEEL engine
  }

  /**
   * Set the factory to create a {@link FeelEngine}
   *
   * @param feelEngineFactory the feel engine factory
   * @return this
   */
  public DefaultDmnEngineConfiguration feelEngineFactory(final FeelEngineFactory feelEngineFactory) {
    setFeelEngineFactory(feelEngineFactory);
    return this;
  }

  /**
   * The feel engine used by the engine. Is initialized during the build of the engine.
   *
   * @return the feel engine
   */
  public FeelEngine getFeelEngine() {
    return feelEngine;
  }

  /**
   * @return the default expression language for input expressions
   */
  public String getDefaultInputExpressionExpressionLanguage() {
    return defaultInputExpressionExpressionLanguage;
  }

  /**
   * Set the default expression language which is used to evaluate input expressions. It is used for
   * all input expressions which do not have a expression language set.
   *
   * @param expressionLanguage the default expression language for input expressions
   */
  public void setDefaultInputExpressionExpressionLanguage(final String expressionLanguage) {
    defaultInputExpressionExpressionLanguage = expressionLanguage;
  }

  /**
   * Set the default expression language which is used to evaluate input expressions. It is used for
   * all input expressions which do not have a expression language set.
   *
   * @param expressionLanguage the default expression language for input expressions
   * @return this configuration
   */
  public DefaultDmnEngineConfiguration defaultInputExpressionExpressionLanguage(
    final String expressionLanguage) {
    setDefaultInputExpressionExpressionLanguage(expressionLanguage);
    return this;
  }

  /**
   * @return the default expression language for input entries
   */
  public String getDefaultInputEntryExpressionLanguage() {
    return defaultInputEntryExpressionLanguage;
  }

  /**
   * Set the default expression language which is used to evaluate input entries. It is used for all
   * input entries which do not have a expression language set.
   *
   * @param expressionLanguage the default expression language for input entries
   */
  public void setDefaultInputEntryExpressionLanguage(final String expressionLanguage) {
    defaultInputEntryExpressionLanguage = expressionLanguage;
  }

  /**
   * Set the default expression language which is used to evaluate input entries. It is used for all
   * input entries which do not have a expression language set.
   *
   * @param expressionLanguage the default expression language for input entries
   * @return this configuration
   */
  public DefaultDmnEngineConfiguration defaultInputEntryExpressionLanguage(
    final String expressionLanguage) {
    setDefaultInputEntryExpressionLanguage(expressionLanguage);
    return this;
  }

  /**
   * @return the default expression language for output entries
   */
  public String getDefaultOutputEntryExpressionLanguage() {
    return defaultOutputEntryExpressionLanguage;
  }

  /**
   * Set the default expression language which is used to evaluate output entries. It is used for
   * all output entries which do not have a expression language set.
   *
   * @param expressionLanguage the default expression language for output entries
   */
  public void setDefaultOutputEntryExpressionLanguage(final String expressionLanguage) {
    defaultOutputEntryExpressionLanguage = expressionLanguage;
  }

  /**
   * Set the default expression language which is used to evaluate output entries. It is used for
   * all output entries which do not have a expression language set.
   *
   * @param expressionLanguage the default expression language for output entries
   * @return this configuration
   */
  public DefaultDmnEngineConfiguration defaultOutputEntryExpressionLanguage(
    final String expressionLanguage) {
    setDefaultOutputEntryExpressionLanguage(expressionLanguage);
    return this;
  }

  /**
   * @return the default expression language for literal expressions
   */
  public String getDefaultLiteralExpressionLanguage() {
    return defaultLiteralExpressionLanguage;
  }

  /**
   * Set the default expression language which is used to evaluate literal expressions. It is used
   * for all literal expressions which do not have a expression language set.
   *
   * @param expressionLanguage the default expression language for literal expressions
   */
  public void setDefaultLiteralExpressionLanguage(final String expressionLanguage) {
    defaultLiteralExpressionLanguage = expressionLanguage;
  }

  /**
   * Set the default expression language which is used to evaluate literal expressions. It is used
   * for all literal expressions which do not have a expression language set.
   *
   * @param expressionLanguage the default expression language for literal expressions
   * @return this configuration
   */
  public DefaultDmnEngineConfiguration defaultLiteralExpressionLanguage(final String expressionLanguage) {
    setDefaultLiteralExpressionLanguage(expressionLanguage);
    return this;
  }

  /**
   * @return the DMN transformer
   */
  public DmnTransformer getTransformer() {
    return transformer;
  }

  /**
   * Set the DMN transformer used to transform the DMN model.
   *
   * @param transformer the DMN transformer
   */
  public void setTransformer(final DmnTransformer transformer) {
    this.transformer = transformer;
  }

  /**
   * Set the DMN transformer used to transform the DMN model.
   *
   * @param transformer the DMN transformer
   * @return this
   */
  public DefaultDmnEngineConfiguration transformer(final DmnTransformer transformer) {
    setTransformer(transformer);
    return this;
  }

  /**
   * @return the list of FEEL Custom Function Providers
   */
  public List<FeelCustomFunctionProvider> getFeelCustomFunctionProviders() {
    return feelCustomFunctionProviders;
  }

  /**
   * Set a list of FEEL Custom Function Providers.
   *
   * @param feelCustomFunctionProviders a list of FEEL Custom Function Providers
   */
  public void setFeelCustomFunctionProviders(
    final List<FeelCustomFunctionProvider> feelCustomFunctionProviders) {
    this.feelCustomFunctionProviders = feelCustomFunctionProviders;
  }

  /**
   * Set a list of FEEL Custom Function Providers.
   *
   * @param feelCustomFunctionProviders a list of FEEL Custom Function Providers
   * @return this
   */
  public DefaultDmnEngineConfiguration feelCustomFunctionProviders(
    final List<FeelCustomFunctionProvider> feelCustomFunctionProviders) {
    setFeelCustomFunctionProviders(feelCustomFunctionProviders);
    return this;
  }

  /**
   * @return whether FEEL legacy behavior is enabled or not
   */
  public boolean isEnableFeelLegacyBehavior() {
    return enableFeelLegacyBehavior;
  }

  /**
   * Controls whether the FEEL legacy behavior is enabled or not
   *
   * @param enableFeelLegacyBehavior the FEEL legacy behavior
   */
  public void setEnableFeelLegacyBehavior(final boolean enableFeelLegacyBehavior) {
    this.enableFeelLegacyBehavior = enableFeelLegacyBehavior;
  }

  /**
   * Controls whether the FEEL legacy behavior is enabled or not
   *
   * @param enableFeelLegacyBehavior the FEEL legacy behavior
   * @return this
   */
  public DefaultDmnEngineConfiguration enableFeelLegacyBehavior(final boolean enableFeelLegacyBehavior) {
    setEnableFeelLegacyBehavior(enableFeelLegacyBehavior);
    return this;
  }

  /**
   * @return whether blank table outputs are swallowed or returned as {@code null}.
   */
  public boolean isReturnBlankTableOutputAsNull() {
    return returnBlankTableOutputAsNull;
  }

  /**
   * Controls whether blank table outputs are swallowed or returned as {@code null}.
   *
   * @param returnBlankTableOutputAsNull toggles whether blank table outputs are swallowed or
   *     returned as {@code null}.
   * @return this
   */
  public DefaultDmnEngineConfiguration setReturnBlankTableOutputAsNull(
    final boolean returnBlankTableOutputAsNull) {
    this.returnBlankTableOutputAsNull = returnBlankTableOutputAsNull;
    return this;
  }
}
