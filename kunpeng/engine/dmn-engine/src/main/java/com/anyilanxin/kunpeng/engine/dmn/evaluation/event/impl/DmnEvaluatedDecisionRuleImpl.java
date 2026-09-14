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

package com.anyilanxin.kunpeng.engine.dmn.evaluation.event.impl;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableRuleImpl;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedDecisionRule;
import com.anyilanxin.kunpeng.engine.dmn.evaluation.event.DmnEvaluatedOutput;
import java.util.Map;

public class DmnEvaluatedDecisionRuleImpl implements DmnEvaluatedDecisionRule {

  protected String id;
  protected Map<String, DmnEvaluatedOutput> outputEntries;

  public DmnEvaluatedDecisionRuleImpl(final DmnDecisionTableRuleImpl matchingRule) {
    id = matchingRule.getKey();
  }

  @Override
  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  @Override
  public Map<String, DmnEvaluatedOutput> getOutputEntries() {
    return outputEntries;
  }

  public void setOutputEntries(final Map<String, DmnEvaluatedOutput> outputEntries) {
    this.outputEntries = outputEntries;
  }

  @Override
  public String toString() {
    return "DmnEvaluatedDecisionRuleImpl{"
        + "id='"
        + id
        + '\''
        + ", outputEntries="
        + outputEntries
        + '}';
  }
}
