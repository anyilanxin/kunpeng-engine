/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.engine.dmn.test;

import static com.anyilanxin.kunpeng.bpm.parse.dmn.DmnFactory.createExpressionLanguage;

import com.anyilanxin.kunpeng.bpm.parse.dmn.DmnFactory;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecision;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecisionRequirementsGraph;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.DmnTransformer;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngine;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngineFactory;
import com.anyilanxin.kunpeng.engine.dmn.util.IoUtil;
import java.io.InputStream;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit test rule for internal unit tests. Uses The {@link DecisionResource} annotation to load
 * decisions before tests.
 *
 * <p>加载流程：DMN 资源经 model-parse 的 {@link DmnTransformer} 转换为 {@link
 * DmnDecisionRequirementsGraph}，引擎按 {@link DmnEngineFactory}（可空，空即默认配置）构建。
 */
public class DmnEngineTestRule implements TestRule {

  public static final String DMN_SUFFIX = "dmn";

  private final DmnEngineFactory engineFactory;

  protected DmnEngine dmnEngine;
  protected DmnDecisionRequirementsGraph drg;
  protected DmnDecision decision;

  public DmnEngineTestRule() {
    this(null);
  }

  public DmnEngineTestRule(final DmnEngineFactory engineFactory) {
    this.engineFactory = engineFactory;
  }

  public DmnEngine getDmnEngine() {
    return dmnEngine;
  }

  /** 本次加载的决策需求图（未标注 {@link DecisionResource} 时为 null）。 */
  public DmnDecisionRequirementsGraph getDrg() {
    return drg;
  }

  public DmnDecision getDecision() {
    return decision;
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        starting(description);
        try {
          base.evaluate();
        } finally {
          finished();
        }
      }
    };
  }

  protected void starting(final Description description) {
    final DmnEngineFactory factory =
        engineFactory != null ? engineFactory : new DmnEngineFactory();
    dmnEngine = factory.build();
    decision = loadDecision(description);
  }

  protected void finished() {
    // 引擎无生命周期资源需要释放
  }

  protected DmnDecision loadDecision(final Description description) {
    final DecisionResource decisionResource = description.getAnnotation(DecisionResource.class);

    if (decisionResource != null) {
      final String resourcePath =
          expandResourcePath(description, decisionResource.resource());
      final InputStream inputStream = IoUtil.fileAsStream(resourcePath);
      final DmnTransformer transformer =
          DmnFactory.createTransformer(createExpressionLanguage(null));
      drg = transformer.transformDefinitions(IoUtil.inputStreamAsByteArray(inputStream));

      final String decisionKey = decisionResource.decisionKey();
      if (decisionKey == null || decisionKey.isEmpty()) {
        return drg.getDecisions().isEmpty() ? null : drg.getDecisions().iterator().next();
      } else {
        return drg.getDecision(decisionKey);
      }
    } else {
      return null;
    }
  }

  protected String expandResourcePath(final Description description, final String resourcePath) {
    if (resourcePath.contains("/")) {
      // already expanded path
      return resourcePath;
    } else {
      final Class<?> testClass = description.getTestClass();
      if (resourcePath.isEmpty()) {
        // use test class and method name as resource file name
        return testClass.getName().replace(".", "/") + "." + description.getMethodName() + "." + DMN_SUFFIX;
      } else {
        // use test class location as resource location
        return testClass.getPackage().getName().replace(".", "/") + "/" + resourcePath;
      }
    }
  }
}
