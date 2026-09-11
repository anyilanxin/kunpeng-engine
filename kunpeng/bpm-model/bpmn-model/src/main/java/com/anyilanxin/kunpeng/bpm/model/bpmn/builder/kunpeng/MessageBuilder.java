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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Message;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengSubscription;

public class MessageBuilder extends AbstractBaseElementBuilder<MessageBuilder, Message> {

  public MessageBuilder(final BpmnModelInstance modelInstance, final Message element) {
    super(modelInstance, element, MessageBuilder.class);
  }

  public MessageBuilder name(final String name) {
    element.setName(name);
    return this;
  }

  public MessageBuilder nameExpression(final String nameExpression) {
    return name(asKunpengExpression(nameExpression));
  }

  public MessageBuilder kunpengCorrelationKey(final String correlationKey) {
    final KunpengSubscription subscription =
        getCreateSingleExtensionElement(KunpengSubscription.class);
    subscription.setCorrelationKey(correlationKey);
    return this;
  }

  public MessageBuilder kunpengCorrelationKeyExpression(final String correlationKeyExpression) {
    return kunpengCorrelationKey(asKunpengExpression(correlationKeyExpression));
  }
}
