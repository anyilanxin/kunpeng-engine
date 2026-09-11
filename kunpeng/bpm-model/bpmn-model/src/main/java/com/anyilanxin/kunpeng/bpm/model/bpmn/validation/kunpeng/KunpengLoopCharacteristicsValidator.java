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

import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.KunpengConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengLoopCharacteristics;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class KunpengLoopCharacteristicsValidator
    implements ModelElementValidator<KunpengLoopCharacteristics> {

  @Override
  public Class<KunpengLoopCharacteristics> getElementType() {
    return KunpengLoopCharacteristics.class;
  }

  @Override
  public void validate(
      final KunpengLoopCharacteristics element,
      final ValidationResultCollector validationResultCollector) {

    final String collection = element.getCollection();
    final String elementVariable = element.getElementVariable();
    if (elementVariable != null && !elementVariable.isEmpty()) {
      if (collection == null || collection.isEmpty()) {
        validationResultCollector.addError(
            0,
            String.format(
                "Attribute '%s' must be present and not empty",
                KunpengConstants.ATTRIBUTE_INPUT_COLLECTION));
      }
    }
  }
}
