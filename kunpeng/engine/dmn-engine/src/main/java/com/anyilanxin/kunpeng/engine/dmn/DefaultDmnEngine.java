/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
