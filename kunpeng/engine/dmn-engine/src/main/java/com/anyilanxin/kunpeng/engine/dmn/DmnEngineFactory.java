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
package com.anyilanxin.kunpeng.engine.dmn;

import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.DefaultHitPolicyHandlerRegistry;
import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.DmnHitPolicyHandlerRegistry;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import com.anyilanxin.kunpeng.engine.script.ScriptEngineFactory;
import io.micrometer.core.instrument.MeterRegistry;

/** DMN 引擎配置。直接 {@code new DmnEngineFactory().buildEngine()} 创建引擎实例。 */
public final class DmnEngineFactory {
  private static final DmnEngineFactory ENGINE_FACTORY = new DmnEngineFactory();
  private boolean returnBlankTableOutputAsNull = false;
  private ScriptEngine feelEngine;
  private DmnHitPolicyHandlerRegistry handlerRegistry;
  private MeterRegistry meterRegistry;

  public static DmnEngineFactory getInstance() {
    return ENGINE_FACTORY;
  }

  public DmnEngineFactory returnBlankTableOutputAsNull(final boolean returnBlankTableOutputAsNull) {
    this.returnBlankTableOutputAsNull = returnBlankTableOutputAsNull;
    return this;
  }

  public DmnEngineFactory meterRegistry(final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    return this;
  }

  public DmnEngineFactory feelEngine(final ScriptEngine feelEngine) {
    this.feelEngine = feelEngine;
    return this;
  }

  private void initFeelEngine() {
    if (feelEngine == null) {
      feelEngine = ScriptEngineFactory.createScriptEngine();
    }
  }

  private void initHitPolicyHandler() {
    if (handlerRegistry == null) {
      handlerRegistry = new DefaultHitPolicyHandlerRegistry();
    }
  }

  /** 创建并初始化 DMN 引擎。 */
  public DmnEngine build() {
    initFeelEngine();
    initHitPolicyHandler();
    return new DefaultDmnEngine(
        feelEngine, handlerRegistry, meterRegistry, returnBlankTableOutputAsNull);
  }
}
