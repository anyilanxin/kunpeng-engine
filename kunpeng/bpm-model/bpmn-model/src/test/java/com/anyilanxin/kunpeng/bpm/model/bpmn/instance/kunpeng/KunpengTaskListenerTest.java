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
package com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstanceTest;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.UserTask;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KunpengTaskListenerTest extends BpmnModelElementInstanceTest {

  @Override
  public TypeAssumption getTypeAssumption() {
    return new TypeAssumption(BpmnModelConstants.KUNPENG_NS, false);
  }

  @Override
  public Collection<ChildElementAssumption> getChildElementAssumptions() {
    return Collections.emptyList();
  }

  @Override
  public Collection<AttributeAssumption> getAttributesAssumptions() {
    return Arrays.asList(
      new AttributeAssumption(BpmnModelConstants.KUNPENG_NS, "eventType", false, true),
      new AttributeAssumption(BpmnModelConstants.KUNPENG_NS, "type", false, true),
      new AttributeAssumption(BpmnModelConstants.KUNPENG_NS, "retries", false, false, "3"));
  }

  @Test
  public void shouldThrowExceptionForInvalidTaskListenerEventType() {
    // given
    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess()
            .startEvent()
            .userTask(
                "my_user_task",
                t ->
                  t.kunpengUserTask()
                    .kunpengTaskListener(l -> l.canceling().type("rejection_listener")))
            .endEvent()
            .done();

    final String modelXml =
        Bpmn.convertToString(modelInstance)
            .replace("eventType=\"canceling\"", "eventType=\"rejection\"");

    // when
    final KunpengTaskListeners taskListeners =
        Bpmn.readModelFromStream(new ByteArrayInputStream(modelXml.getBytes()))
            .<UserTask>getModelElementById("my_user_task")
          .getSingleExtensionElement(KunpengTaskListeners.class);

    // then
    final Optional<KunpengTaskListener> first = taskListeners.getTaskListeners().stream().findFirst();

    assertThat(first).isPresent();
    assertThatThrownBy(() -> first.get().getEventType())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "No enum constant com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskListenerEventType.rejection");
  }
}
