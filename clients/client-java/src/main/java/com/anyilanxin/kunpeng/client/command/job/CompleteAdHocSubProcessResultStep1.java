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
package com.anyilanxin.kunpeng.client.command.job;

import com.anyilanxin.kunpeng.client.command.CommandWithVariables;
import java.io.InputStream;
import java.util.Map;

public interface CompleteAdHocSubProcessResultStep1 extends CompleteJobResult {

  /**
   * Adds an element to activate in the ad-hoc sub-process.
   *
   * @return this result
   */
  CompleteAdHocSubProcessResultStep2 activateElement(String elementId);

  /**
   * Indicates whether the completion condition of the ad-hoc sub-process is fulfilled.
   *
   * @return this result
   */
  CompleteAdHocSubProcessResultStep1 completionConditionFulfilled(
      boolean completionConditionFulfilled);

  /**
   * Indicates whether all remaining instances of the ad-hoc sub-process should be canceled.
   *
   * @return this result
   */
  CompleteAdHocSubProcessResultStep1 cancelRemainingInstances(boolean cancelRemainingInstances);

  interface CompleteAdHocSubProcessResultStep2
      extends CompleteAdHocSubProcessResultStep1,
          CommandWithVariables<CompleteAdHocSubProcessResultStep1> {
    /**
     * The variables that will be created on the activated element instance.
     *
     * @param variables the variables JSON document as String
     * @return the builder for this command.
     */
    @Override
    CompleteAdHocSubProcessResultStep1 variables(String variables);

    /**
     * The variables that will be created on the activated element instance.
     *
     * @param variables the variables document as object to be serialized to JSON
     * @return the builder for this command.
     */
    @Override
    CompleteAdHocSubProcessResultStep1 variables(Object variables);

    /**
     * The variables that will be created on the activated element instance.
     *
     * @param variables the variables JSON document as stream
     * @return the builder for this command.
     */
    @Override
    CompleteAdHocSubProcessResultStep1 variables(InputStream variables);

    /**
     * The variables that will be created on the activated element instance.
     *
     * @param variables the variables document as map
     * @return the builder for this command.
     */
    @Override
    CompleteAdHocSubProcessResultStep1 variables(Map<String, Object> variables);

    /**
     * A single variable that will be created on the activated element instance.
     *
     * @param key the key of the variable as string
     * @param value the value of the variable as object
     * @return the builder for this command.
     */
    @Override
    CompleteAdHocSubProcessResultStep1 variable(String key, Object value);
  }
}
