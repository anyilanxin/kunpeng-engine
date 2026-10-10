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

package com.anyilanxin.kunpeng.engine.dmn;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecisionRequirementsGraph;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.DefaultDmnDecisionEvaluationContext;
import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.DmnHitPolicyHandlerRegistry;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Objects;

public class DefaultDmnEngine implements DmnEngine {
  protected static final DmnEngineLogger LOG = DmnLogger.ENGINE_LOGGER;
  final DefaultDmnDecisionEvaluationContext decisionContext;
  final MeterRegistry meterRegistry;

  public DefaultDmnEngine(
      final ScriptEngine feelEngine,
      final DmnHitPolicyHandlerRegistry policyHandlerRegistry,
      final MeterRegistry meterRegistry,
      final boolean returnBlankTableOutputAsNull) {
    decisionContext =
        new DefaultDmnDecisionEvaluationContext(
            feelEngine, policyHandlerRegistry, returnBlankTableOutputAsNull);
    this.meterRegistry = Objects.requireNonNullElseGet(meterRegistry, SimpleMeterRegistry::new);
  }

  @Override
  public DmnDecisionResult evaluateDecision(
      final DmnDecisionRequirementsGraph requirementsGraph,
      final String decisionId,
      final ScriptContext scriptContext) {
    return decisionContext.evaluateDecision(requirementsGraph, decisionId, scriptContext);
  }
}
