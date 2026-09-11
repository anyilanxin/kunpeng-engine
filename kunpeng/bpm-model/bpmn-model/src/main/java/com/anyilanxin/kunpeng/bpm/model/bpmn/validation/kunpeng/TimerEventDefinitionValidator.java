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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.TimeCycle;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.TimeDate;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.TimeDuration;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.TimerEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultCollector;

public class TimerEventDefinitionValidator implements ModelElementValidator<TimerEventDefinition> {
  @Override
  public Class<TimerEventDefinition> getElementType() {
    return TimerEventDefinition.class;
  }

  @Override
  public void validate(
      final TimerEventDefinition element,
      final ValidationResultCollector validationResultCollector) {
    final TimeDuration timeDuration = element.getTimeDuration();
    final TimeCycle timeCycle = element.getTimeCycle();
    final TimeDate timeDate = element.getTimeDate();
    int definitionsCount = 0;

    if (timeDate != null) {
      definitionsCount++;
    }

    if (timeDuration != null) {
      definitionsCount++;
    }

    if (timeCycle != null) {
      definitionsCount++;
    }

    if (definitionsCount != 1) {
      validationResultCollector.addError(
          0, "Must be exactly one type of timer: timeDuration, timeDate or timeCycle");
    }
  }
}
