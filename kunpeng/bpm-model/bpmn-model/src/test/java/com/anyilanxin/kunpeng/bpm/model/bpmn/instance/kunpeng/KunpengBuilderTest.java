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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.*;
import org.junit.Test;

import java.util.Collection;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

public class KunpengBuilderTest {

  @Test
  public void shouldBuildServiceTask() {
    // when
    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess()
            .startEvent()
            .serviceTask(
                "foo",
                b ->
                  b.kunpengJobType("taskType")
                    .kunpengJobRetries("5")
                    .kunpengTaskHeader("foo", "f")
                    .kunpengTaskHeader("bar", "b"))
            .endEvent()
            .done();

    // then
    Bpmn.validateModel(modelInstance);

    final ServiceTask serviceTask = modelInstance.getModelElementById("foo");

    final KunpengTaskDefinition taskDefinition =
            getExtensionElement(serviceTask, KunpengTaskDefinition.class);
    assertThat(taskDefinition.getType()).isEqualTo("taskType");
    assertThat(taskDefinition.getRetries()).isEqualTo("5");

    final KunpengDynamicAdditions taskHeaders = getExtensionElement(serviceTask, KunpengDynamicAdditions.class);
    final Collection<KunpengAddition> headerCollection = taskHeaders.getHeaders();
    assertThat(headerCollection).hasSize(2);
    assertThat(headerCollection).element(0).matches(header("foo", "f"));
    assertThat(headerCollection).element(1).matches(header("bar", "b"));
  }

  @Test
  public void shouldBuildTaskWithIoMapping() {
    // when
    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess()
            .startEvent()
            .serviceTask(
                "foo",
                b ->
                  b.kunpengInputExpression("inputSource", "inputTarget")
                    .kunpengOutputExpression("outputSource", "outputTarget"))
            .endEvent()
            .done();

    // then
    Bpmn.validateModel(modelInstance);

    final ServiceTask serviceTask = modelInstance.getModelElementById("foo");

    final KunpengIoMapping ioMapping = getExtensionElement(serviceTask, KunpengIoMapping.class);

    final Collection<KunpengInput> inputs = ioMapping.getInputs();
    assertThat(inputs).hasSize(1);
    assertThat(inputs).element(0).matches(mapping("=inputSource", "inputTarget"));

    final Collection<KunpengOutput> outputs = ioMapping.getOutputs();
    assertThat(outputs).hasSize(1);
    assertThat(outputs).element(0).matches(mapping("=outputSource", "outputTarget"));
  }

  @Test
  public void shouldBuildIntermediateMessageCatchEvent() {
    // when
    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess()
            .startEvent()
            .intermediateCatchEvent("catch")
          .message(b -> b.name("messageName").kunpengCorrelationKeyExpression("correlationKey"))
            .endEvent()
            .done();

    // then
    Bpmn.validateModel(modelInstance);

    final IntermediateCatchEvent catchEvent = modelInstance.getModelElementById("catch");
    final Collection<EventDefinition> definitions = catchEvent.getEventDefinitions();
    assertThat(definitions).hasSize(1);

    final EventDefinition eventDefinition = definitions.iterator().next();
    assertThat(eventDefinition).isInstanceOf(MessageEventDefinition.class);

    final MessageEventDefinition messageEventDefinition = (MessageEventDefinition) eventDefinition;
    final Message message = messageEventDefinition.getMessage();

    assertThat(message).isNotNull();
    assertThat(message.getName()).isEqualTo("messageName");

    final KunpengSubscription subscription = getExtensionElement(message, KunpengSubscription.class);
    assertThat(subscription.getCorrelationKey()).isEqualTo("=correlationKey");
  }

  @Test
  public void shouldBuildReceiveTask() {
    // when
    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess()
            .startEvent()
            .receiveTask("catch")
          .message(b -> b.name("messageName").kunpengCorrelationKeyExpression("correlationKey"))
            .endEvent()
            .done();

    // then
    Bpmn.validateModel(modelInstance);

    final ReceiveTask task = modelInstance.getModelElementById("catch");
    final Message message = task.getMessage();

    assertThat(message).isNotNull();
    assertThat(message.getName()).isEqualTo("messageName");

    final KunpengSubscription subscription = getExtensionElement(message, KunpengSubscription.class);
    assertThat(subscription.getCorrelationKey()).isEqualTo("=correlationKey");
  }

  private <T extends BpmnModelElementInstance> T getExtensionElement(
      final BaseElement element, final Class<T> typeClass) {
    final T extensionElement =
        (T) element.getExtensionElements().getUniqueChildElementByType(typeClass);
    assertThat(element).isNotNull();
    return extensionElement;
  }

  private static Predicate<KunpengAddition> header(final String key, final String value) {
    return h -> key.equals(h.getKey()) && value.equals(h.getValue());
  }

  private static Predicate<KunpengMapping> mapping(final String source, final String target) {
    return h -> source.equals(h.getSource()) && target.equals(h.getTarget());
  }
}
