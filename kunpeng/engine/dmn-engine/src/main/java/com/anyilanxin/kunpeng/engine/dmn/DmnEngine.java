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

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecisionRequirementsGraph;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;

/**
 * A DMN engine which can parse DMN decision models and evaluate decisions.
 *
 * <p>A new DMN engine can be build with a DMN engine configuration (see {@link
 * DmnEngineFactory#build()}).
 */
public interface DmnEngine {
  /**
   * Evaluates a decision. The decision can be implemented as any kind of supported decision logic
   * (e.g., decision table, literal expression).
   *
   * @param requirementsGraph the {@link DmnDecisionRequirementsGraph}
   * @param decisionId decision id
   * @param scriptContext the {@link ScriptContext} which are available during the evaluation of
   *     expressions
   * @return the {@link DmnDecisionResult} of this evaluation
   */
  DmnDecisionResult evaluateDecision(
      DmnDecisionRequirementsGraph requirementsGraph,
      String decisionId,
      ScriptContext scriptContext);
}
