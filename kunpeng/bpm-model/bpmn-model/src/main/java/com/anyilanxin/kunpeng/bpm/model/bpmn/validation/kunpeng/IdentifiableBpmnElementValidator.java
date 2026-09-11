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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IdentifiableBpmnElement;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class IdentifiableBpmnElementValidator {

  public static <T extends IdentifiableBpmnElement> void validate(
      final T element, final ValidationResultCollector validationResultCollector) {
    if (element.getId() == null || element.getId().isEmpty()) {
      validationResultCollector.addError(0, "Element id must be present and not empty.");
    }
  }
}
