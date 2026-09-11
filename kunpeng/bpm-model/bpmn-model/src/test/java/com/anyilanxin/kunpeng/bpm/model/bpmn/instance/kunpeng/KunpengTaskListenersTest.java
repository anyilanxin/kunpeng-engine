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
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstanceTest;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExtensionElements;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskListener.DEFAULT_RETRIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class KunpengTaskListenersTest extends BpmnModelElementInstanceTest {

    @Override
    public TypeAssumption getTypeAssumption() {
      return new TypeAssumption(BpmnModelConstants.KUNPENG_NS, false);
    }

    @Override
    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Arrays.asList(
          new ChildElementAssumption(BpmnModelConstants.KUNPENG_NS, KunpengTaskListener.class));
    }

    @Override
    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Collections.emptyList();
    }

    @Test
    public void shouldReadTaskListenerElements() {
        // given
        modelInstance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .userTask(
                                "my_user_task",
                                t ->
                                  t.kunpengUserTask()
                                    .kunpengTaskListener(l -> l.creating().type("create_listener").retries("2"))
                                    .kunpengTaskListener(l -> l.updating().type("update_listener"))
                                    .kunpengTaskListener(
                                                        l -> l.updating().type("update_listener_2").retries("33"))
                                    .kunpengTaskListener(
                                                        l -> l.assigning().type("assignment_listener").retries("4"))
                                    .kunpengTaskListener(
                                                        l -> l.completing().type("complete_listener").retries("5"))
                                    .kunpengTaskListener(l -> l.canceling().type("cancel_listener").retries("6")))
                        .endEvent()
                        .done();

        final ModelElementInstance userTask = modelInstance.getModelElementById("my_user_task");

        // when
      final Collection<KunpengTaskListener> taskListeners = getTaskListeners(userTask);

        // then
        assertThat(taskListeners)
                .extracting("eventType", "type", "retries")
                .containsExactly(
                  tuple(KunpengTaskListenerEventType.creating, "create_listener", "2"),
                  tuple(KunpengTaskListenerEventType.updating, "update_listener", DEFAULT_RETRIES),
                  tuple(KunpengTaskListenerEventType.updating, "update_listener_2", "33"),
                  tuple(KunpengTaskListenerEventType.assigning, "assignment_listener", "4"),
                  tuple(KunpengTaskListenerEventType.completing, "complete_listener", "5"),
                  tuple(KunpengTaskListenerEventType.canceling, "cancel_listener", "6"));
    }

  private Collection<KunpengTaskListener> getTaskListeners(
            final ModelElementInstance elementInstance) {
        return elementInstance
                .getUniqueChildElementByType(ExtensionElements.class)
          .getUniqueChildElementByType(KunpengTaskListeners.class)
          .getChildElementsByType(KunpengTaskListener.class);
    }
}
