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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengLoopCharacteristics;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class KunpengLoopCharacteristicsImpl extends BpmnModelElementInstanceImpl
    implements KunpengLoopCharacteristics {

  private static Attribute<String> collectionAttribute;

  private static Attribute<String> elementVariableAttribute;

  public KunpengLoopCharacteristicsImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(
                KunpengLoopCharacteristics.class, KunpengConstants.ELEMENT_LOOP_CHARACTERISTICS)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengLoopCharacteristicsImpl::new);

    collectionAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_COLLECTION)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .required()
            .build();

    elementVariableAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_ELEMENT_VARIABLE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    typeBuilder.build();
  }

  @Override
  public String getCollection() {
    return collectionAttribute.getValue(this);
  }

  @Override
  public void setCollection(final String collection) {
    collectionAttribute.setValue(this, collection);
  }

  @Override
  public String getElementVariable() {
    return elementVariableAttribute.getValue(this);
  }

  @Override
  public void setElementVariable(final String elementVariable) {
    elementVariableAttribute.setValue(this, elementVariable);
  }
}
