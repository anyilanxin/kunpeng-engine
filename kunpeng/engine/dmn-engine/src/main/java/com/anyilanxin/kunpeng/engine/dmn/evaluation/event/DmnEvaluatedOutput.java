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

/**
 * The output for a evaluated decision.
 *
 * <p>In a decision table implementation an output can have a human readable name and a name which
 * can be used to reference the output value in the decision result.
 *
 * <p>The human readable name is the {@code label} attribute of the DMN XML {@code output} element.
 * You can access this name by the {@link #getName()} getter.
 *
 * <p>The output name to reference the output value in the decision result is the {@code name}
 * attribute of the DMN XML {@code output} element. You can access this output name by the {@link
 * #getOutputName()} getter.
 *
 * <p>The {@code id} and {@code value} of the evaluated decision table output entry can be access by
 * the {@link #getId()} and {@link #getValue()} getter.
 */
public interface DmnEvaluatedOutput {

  /**
   * @return the id of the evaluated output or null if not set
   */
  String getId();

  /**
   * @return the name of the evaluated output or null if not set
   */
  String getName();

  /**
   * @return the output name of the evaluated output or null if not set
   */
  String getOutputName();

  /**
   * @return the value of the evaluated output or null if non set
   */
  TypedValue getValue();
}
