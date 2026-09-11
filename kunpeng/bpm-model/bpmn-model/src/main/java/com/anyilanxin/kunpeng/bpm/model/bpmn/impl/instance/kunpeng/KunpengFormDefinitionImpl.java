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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengFormDefinition;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class KunpengFormDefinitionImpl extends BpmnModelElementInstanceImpl
    implements KunpengFormDefinition {

  protected static Attribute<String> formKeyAttribute;
  protected static Attribute<String> formIdAttribute;
  protected static Attribute<String> externalReferenceAttribute;
  private static Attribute<KunpengBindingType> bindingTypeAttribute;
  private static Attribute<String> versionTagAttribute;

  public KunpengFormDefinitionImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getFormKey() {
    return formKeyAttribute.getValue(this);
  }

  @Override
  public void setFormKey(final String formKey) {
    formKeyAttribute.setValue(this, formKey);
  }

  @Override
  public String getFormId() {
    return formIdAttribute.getValue(this);
  }

  @Override
  public void setFormId(final String formId) {
    formIdAttribute.setValue(this, formId);
  }

  @Override
  public String getExternalReference() {
    return externalReferenceAttribute.getValue(this);
  }

  @Override
  public void setExternalReference(final String externalReference) {
    externalReferenceAttribute.setValue(this, externalReference);
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
            .defineType(KunpengFormDefinition.class, KunpengConstants.ELEMENT_FORM_DEFINITION)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengFormDefinitionImpl::new);

    formKeyAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_FORM_KEY)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    formIdAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_FORM_ID)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    externalReferenceAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_EXTERNAL_REFERENCE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
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
