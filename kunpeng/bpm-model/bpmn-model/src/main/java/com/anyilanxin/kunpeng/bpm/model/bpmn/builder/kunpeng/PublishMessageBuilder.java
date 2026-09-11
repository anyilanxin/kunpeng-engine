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
package com.anyilanxin.kunpeng.bpm.model.bpmn.builder.kunpeng;

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.AbstractBaseElementBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BaseElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Message;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengPublishMessage;
import java.util.function.Consumer;

public class PublishMessageBuilder
    extends AbstractBaseElementBuilder<PublishMessageBuilder, BaseElement> {
  public final Consumer<Message> consumer;

  public PublishMessageBuilder(
      final BpmnModelInstance modelInstance,
      final BaseElement element,
      final Consumer<Message> consumer) {
    super(modelInstance, element, PublishMessageBuilder.class);
    this.consumer = consumer;
  }

  public PublishMessageBuilder name(final String name) {
    consumer.accept(findMessageForName(name));
    return this;
  }

  public PublishMessageBuilder nameExpression(final String nameExpression) {
    return name(asKunpengExpression(nameExpression));
  }

  /**
   * Sets a static correlation key of the message.
   *
   * @param correlationKey the correlation key of the message
   * @return the builder object
   */
  public PublishMessageBuilder kunpengCorrelationKey(final String correlationKey) {
    final KunpengPublishMessage publishMessage =
        getCreateSingleExtensionElement(KunpengPublishMessage.class);
    publishMessage.setCorrelationKey(correlationKey);

    return myself;
  }

  /**
   * Sets a dynamic correlation key of the message. The correlation key is retrieved from the given
   * expression.
   *
   * @param correlationKeyExpression the expression for the correlation key of the message
   * @return the builder object
   */
  public PublishMessageBuilder kunpengCorrelationKeyExpression(
      final String correlationKeyExpression) {
    return kunpengCorrelationKey(asKunpengExpression(correlationKeyExpression));
  }

  /**
   * Sets a static time to live of the message.
   *
   * @param timeToLive the correlation key of the message
   * @return the builder object
   */
  public PublishMessageBuilder kunpengTimeToLive(final String timeToLive) {
    final KunpengPublishMessage publishMessage =
        getCreateSingleExtensionElement(KunpengPublishMessage.class);
    publishMessage.setTimeToLive(timeToLive);

    return myself;
  }

  /**
   * Sets a dynamic time to live of the message. The time to live is retrieved from the given
   * expression.
   *
   * @param timeToLiveExpression the expression for the time to live of the message
   * @return the builder object
   */
  public PublishMessageBuilder kunpengTimeToLiveExpression(final String timeToLiveExpression) {
    return kunpengTimeToLive(asKunpengExpression(timeToLiveExpression));
  }
}
