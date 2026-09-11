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
package com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.kunpeng;

import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.KunpengConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengPublishMessage;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class KunpengPublishMessageImpl extends BpmnModelElementInstanceImpl
    implements KunpengPublishMessage {

  private static Attribute<String> correlationKeyAttribute;
  private static Attribute<String> timeToLiveAttribute;

  public KunpengPublishMessageImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(KunpengPublishMessage.class, KunpengConstants.ELEMENT_PUBLISH_MESSAGE)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengPublishMessageImpl::new);

    correlationKeyAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_CORRELATION_KEY)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .required()
            .build();

    timeToLiveAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_MESSAGE_TIME_TO_LIVE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    typeBuilder.build();
  }

  @Override
  public String getCorrelationKey() {
    return correlationKeyAttribute.getValue(this);
  }

  @Override
  public void setCorrelationKey(final String correlationKey) {
    correlationKeyAttribute.setValue(this, correlationKey);
  }

  @Override
  public String getTimeToLive() {
    return timeToLiveAttribute.getValue(this);
  }

  @Override
  public void setTimeToLive(final String timeToLive) {
    timeToLiveAttribute.setValue(this, timeToLive);
  }
}
