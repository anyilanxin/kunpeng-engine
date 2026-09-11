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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.LoopCardinality;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengLoopCharacteristics;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public final class MultiInstanceLoopCharacteristicsValidator
    implements ModelElementValidator<MultiInstanceLoopCharacteristics> {

  @Override
  public Class<MultiInstanceLoopCharacteristics> getElementType() {
    return MultiInstanceLoopCharacteristics.class;
  }

  @Override
  public void validate(
      final MultiInstanceLoopCharacteristics element,
      final ValidationResultCollector validationResultCollector) {
    final LoopCardinality loopCardinality = element.getLoopCardinality();
    final KunpengLoopCharacteristics loopCharacteristics =
        element.getSingleExtensionElement(KunpengLoopCharacteristics.class);
    if (loopCharacteristics == null && loopCardinality == null) {
      validationResultCollector.addError(0, "Must have Loop cardinality or Collection");
      return;
    }
    boolean haveLoopInfo = false;
    if (loopCardinality != null) {
      final String textContent = loopCardinality.getTextContent();
      if (textContent != null && !textContent.trim().isEmpty()) {
        haveLoopInfo = true;
      }
    }

    if (loopCharacteristics != null) {
      final String collection = loopCharacteristics.getCollection();
      if (collection != null && !collection.trim().isEmpty()) {
        haveLoopInfo = true;
      }
    }
    if (!haveLoopInfo) {
      validationResultCollector.addError(0, "Must have Loop cardinality or Collection");
    }
  }
}
