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
package com.anyilanxin.kunpeng.bpm.model.bpmn.validation.kunpeng;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengPriorityDefinition;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class KunpengPriorityDefinitionValidator
    implements ModelElementValidator<KunpengPriorityDefinition> {

  public static final Integer PRIORITY_LOWER_BOUND = 0;
  public static final Integer PRIORITY_UPPER_BOUND = 100;

  @Override
  public Class<KunpengPriorityDefinition> getElementType() {
    return KunpengPriorityDefinition.class;
  }

  @Override
  public void validate(
      final KunpengPriorityDefinition kunpengPriorityDefinition,
      final ValidationResultCollector validationResultCollector) {
    final String priority = kunpengPriorityDefinition.getPriority();

    try {
      final int priorityValue = Integer.parseInt(priority);
      if (priorityValue < PRIORITY_LOWER_BOUND || priorityValue > PRIORITY_UPPER_BOUND) {
        validationResultCollector.addError(
            0,
            String.format("Priority must be a number between 0 and 100, but was '%s'.", priority));
      }
    } catch (final NumberFormatException ignored) {
      /* Handled by previous runtime validation step */
    }
  }
}
