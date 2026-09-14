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

package com.anyilanxin.kunpeng.engine.dmn.evaluation.event;

import java.util.Map;

/** A evaluated decision rule. */
public interface DmnEvaluatedDecisionRule {

  /**
   * @return the id of the decision rule or null if not set
   */
  String getId();

  /**
   * @return the evaluated output entries for the decision rule
   */
  Map<String, DmnEvaluatedOutput> getOutputEntries();
}
