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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExtensionElements;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ScriptTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengScript;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Collection;

public final class ScriptTaskValidator implements ModelElementValidator<ScriptTask> {

  @Override
  public Class<ScriptTask> getElementType() {
    return ScriptTask.class;
  }

  @Override
  public void validate(
      final ScriptTask element, final ValidationResultCollector validationResultCollector) {
    IdentifiableBpmnElementValidator.validate(element, validationResultCollector);

    if (!hasExactlyOneExtension(element)) {
      validationResultCollector.addError(
          0,
          String.format(
              "Must have either one 'kunpeng:%s' or one 'kunpeng:%s' extension element",
              KunpengConstants.ELEMENT_SCRIPT, KunpengConstants.ELEMENT_TASK_DEFINITION));
    }
  }

  private boolean hasExactlyOneExtension(final ScriptTask element) {
    final ExtensionElements extensionElements = element.getExtensionElements();

    if (extensionElements == null) {
      return false;
    }

    final Collection<KunpengScript> scriptExtensions =
        extensionElements.getChildElementsByType(KunpengScript.class);
    final Collection<KunpengTaskDefinition> taskDefinitionExtensions =
        extensionElements.getChildElementsByType(KunpengTaskDefinition.class);

    return scriptExtensions.size() == 1 && taskDefinitionExtensions.isEmpty()
        || scriptExtensions.isEmpty() && taskDefinitionExtensions.size() == 1;
  }
}
