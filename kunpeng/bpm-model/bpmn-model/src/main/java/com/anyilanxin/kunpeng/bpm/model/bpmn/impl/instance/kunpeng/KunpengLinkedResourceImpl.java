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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengLinkedResource;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class KunpengLinkedResourceImpl extends BpmnModelElementInstanceImpl
    implements KunpengLinkedResource {
  private static Attribute<String> resourceIdAttribute;
  private static Attribute<KunpengBindingType> bindingTypeAttribute;
  private static Attribute<String> resourceTypeAttribute;
  private static Attribute<String> versionTagAttribute;
  private static Attribute<String> linkNameAttribute;

  public KunpengLinkedResourceImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(KunpengLinkedResource.class, KunpengConstants.ELEMENT_LINKED_RESOURCE)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengLinkedResourceImpl::new);

    resourceIdAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_RESOURCE_ID)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    bindingTypeAttribute =
        typeBuilder
            .enumAttribute(KunpengConstants.ATTRIBUTE_BINDING_TYPE, KunpengBindingType.class)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .defaultValue(KunpengBindingType.latest)
            .build();

    resourceTypeAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_RESOURCE_TYPE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    versionTagAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_VERSION_TAG)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    linkNameAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_LINK_NAME)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    typeBuilder.build();
  }

  @Override
  public String getResourceId() {
    return resourceIdAttribute.getValue(this);
  }

  @Override
  public void setResourceId(final String resourceId) {
    resourceIdAttribute.setValue(this, resourceId);
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
  public String getResourceType() {
    return resourceTypeAttribute.getValue(this);
  }

  @Override
  public void setResourceType(final String resourceType) {
    resourceTypeAttribute.setValue(this, resourceType);
  }

  @Override
  public String getVersionTag() {
    return versionTagAttribute.getValue(this);
  }

  @Override
  public void setVersionTag(final String versionTag) {
    versionTagAttribute.setValue(this, versionTag);
  }

  @Override
  public String getLinkName() {
    return linkNameAttribute.getValue(this);
  }

  @Override
  public void setLinkName(final String linkName) {
    linkNameAttribute.setValue(this, linkName);
  }
}
