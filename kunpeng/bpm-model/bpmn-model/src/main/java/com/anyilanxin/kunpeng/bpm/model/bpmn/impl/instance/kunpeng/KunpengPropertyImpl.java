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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengProperty;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class KunpengPropertyImpl extends BpmnModelElementInstanceImpl implements KunpengProperty {

  private static Attribute<String> nameAttribute;
  private static Attribute<String> valueAttribute;

  public KunpengPropertyImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getName() {
    return nameAttribute.getValue(this);
  }

  @Override
  public void setName(final String name) {
    nameAttribute.setValue(this, name);
  }

  @Override
  public String getValue() {
    return valueAttribute.getValue(this);
  }

  @Override
  public void setValue(final String value) {
    valueAttribute.setValue(this, value);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(KunpengProperty.class, KunpengConstants.ELEMENT_PROPERTY)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengPropertyImpl::new);

    nameAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_NAME)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .required()
            .build();

    valueAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_VALUE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .required()
            .build();

    typeBuilder.build();
  }
}
