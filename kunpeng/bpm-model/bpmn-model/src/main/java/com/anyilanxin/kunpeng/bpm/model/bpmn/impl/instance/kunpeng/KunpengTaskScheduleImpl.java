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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskSchedule;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class KunpengTaskScheduleImpl extends BpmnModelElementInstanceImpl
    implements KunpengTaskSchedule {

  private static Attribute<String> dueDateAttribute;
  private static Attribute<String> followUpDateAttribute;

  public KunpengTaskScheduleImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(KunpengTaskSchedule.class, KunpengConstants.ELEMENT_SCHEDULE_DEFINITION)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengTaskScheduleImpl::new);

    dueDateAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_DUE_DATE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    followUpDateAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_FOLLOW_UP_DATE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    typeBuilder.build();
  }

  @Override
  public String getDueDate() {
    return dueDateAttribute.getValue(this);
  }

  @Override
  public void setDueDate(final String dueDate) {
    dueDateAttribute.setValue(this, dueDate);
  }

  @Override
  public String getFollowUpDate() {
    return followUpDateAttribute.getValue(this);
  }

  @Override
  public void setFollowUpDate(final String followUpDate) {
    followUpDateAttribute.setValue(this, followUpDate);
  }
}
