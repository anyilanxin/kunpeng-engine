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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengBindingType;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengCalledElement;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class KunpengCalledElementImpl extends BpmnModelElementInstanceImpl
    implements KunpengCalledElement {

  private static Attribute<String> processIdAttribute;
  private static Attribute<Boolean> propagateAllChildVariablesAttribute;
  private static Attribute<Boolean> propagateAllParentVariablesAttribute;
  private static Attribute<KunpengBindingType> bindingTypeAttribute;
  private static Attribute<String> versionTagAttribute;

  public KunpengCalledElementImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getProcessId() {
    return processIdAttribute.getValue(this);
  }

  @Override
  public void setProcessId(final String processId) {
    processIdAttribute.setValue(this, processId);
  }

  @Override
  public boolean isPropagateAllChildVariablesEnabled() {
    return propagateAllChildVariablesAttribute.getValue(this);
  }

  @Override
  public void setPropagateAllChildVariablesEnabled(
      final boolean propagateAllChildVariablesEnabled) {
    propagateAllChildVariablesAttribute.setValue(this, propagateAllChildVariablesEnabled);
  }

  @Override
  public boolean isPropagateAllParentVariablesEnabled() {
    return propagateAllParentVariablesAttribute.getValue(this);
  }

  @Override
  public void setPropagateAllParentVariablesEnabled(
      final boolean propagateAllParentVariablesEnabled) {
    propagateAllParentVariablesAttribute.setValue(this, propagateAllParentVariablesEnabled);
  }

  @Override
  public KunpengBindingType getBindingType() {
    return bindingTypeAttribute.getValue(this);
  }

  @Override
  public void setBindingType(final KunpengBindingType bindingType) {
    bindingTypeAttribute.setValue(this, bindingType);
  }

  @Override
  public String getVersionTag() {
    return versionTagAttribute.getValue(this);
  }

  @Override
  public void setVersionTag(final String versionTag) {
    versionTagAttribute.setValue(this, versionTag);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(KunpengCalledElement.class, KunpengConstants.ELEMENT_CALLED_ELEMENT)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengCalledElementImpl::new);

    processIdAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_PROCESS_ID)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    propagateAllChildVariablesAttribute =
        typeBuilder
            .booleanAttribute(KunpengConstants.ATTRIBUTE_PROPAGATE_ALL_CHILD_VARIABLES)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .defaultValue(true)
            .build();

    propagateAllParentVariablesAttribute =
        typeBuilder
            .booleanAttribute(KunpengConstants.ATTRIBUTE_PROPAGATE_ALL_PARENT_VARIABLES)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .defaultValue(true)
            .build();

    bindingTypeAttribute =
        typeBuilder
            .enumAttribute(KunpengConstants.ATTRIBUTE_BINDING_TYPE, KunpengBindingType.class)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .defaultValue(KunpengBindingType.latest)
            .build();

    versionTagAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_VERSION_TAG)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    typeBuilder.build();
  }
}
