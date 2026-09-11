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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskListener;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskListenerEventType;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class KunpengTaskListenerImpl extends BpmnModelElementInstanceImpl
    implements KunpengTaskListener {

  protected static Attribute<KunpengTaskListenerEventType> eventTypeAttribute;
  protected static Attribute<String> typeAttribute;
  protected static Attribute<String> retriesAttribute;

  public KunpengTaskListenerImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public KunpengTaskListenerEventType getEventType() {
    return eventTypeAttribute.getValue(this);
  }

  @Override
  public void setEventType(final KunpengTaskListenerEventType eventType) {
    eventTypeAttribute.setValue(this, eventType);
  }

  @Override
  public String getType() {
    return typeAttribute.getValue(this);
  }

  @Override
  public void setType(final String type) {
    typeAttribute.setValue(this, type);
  }

  @Override
  public String getRetries() {
    return retriesAttribute.getValue(this);
  }

  @Override
  public void setRetries(final String retries) {
    retriesAttribute.setValue(this, retries);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(KunpengTaskListener.class, KunpengConstants.ELEMENT_TASK_LISTENER)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengTaskListenerImpl::new);

    eventTypeAttribute =
        typeBuilder
            .enumAttribute(
                KunpengConstants.ATTRIBUTE_EVENT_TYPE, KunpengTaskListenerEventType.class)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .required()
            .build();

    typeAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_TYPE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .required()
            .build();

    retriesAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_RETRIES)
            .defaultValue(KunpengTaskListener.DEFAULT_RETRIES)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    typeBuilder.build();
  }
}
