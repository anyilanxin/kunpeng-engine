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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Definitions;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;
import java.util.Collection;

public class DefinitionsValidator implements ModelElementValidator<Definitions> {

  @Override
  public Class<Definitions> getElementType() {
    return Definitions.class;
  }

  @Override
  public void validate(
      final Definitions element, final ValidationResultCollector validationResultCollector) {
    final Collection<Process> processes = element.getChildElementsByType(Process.class);

    if (processes.stream().noneMatch(Process::isExecutable)) {
      validationResultCollector.addError(0, "Must contain at least one executable process");
    }
  }
}
