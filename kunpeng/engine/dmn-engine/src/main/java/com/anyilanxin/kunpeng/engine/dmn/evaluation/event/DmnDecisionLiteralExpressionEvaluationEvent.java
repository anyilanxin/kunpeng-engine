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

import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;

/** Event which represents the evaluation of a decision with a literal expression. */
public interface DmnDecisionLiteralExpressionEvaluationEvent
    extends DmnDecisionLogicEvaluationEvent {

  /**
   * @return the output name of the evaluated expression
   */
  String getOutputName();

  /**
   * @return the value of the evaluated expression
   */
  TypedValue getOutputValue();
}
