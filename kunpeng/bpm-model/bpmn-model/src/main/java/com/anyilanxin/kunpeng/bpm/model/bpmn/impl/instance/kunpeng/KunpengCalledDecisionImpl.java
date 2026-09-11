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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengCalledDecision;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class KunpengCalledDecisionImpl extends BpmnModelElementInstanceImpl
    implements KunpengCalledDecision {

  private static Attribute<String> decisionIdAttribute;
  private static Attribute<String> resultVariableAttribute;
  private static Attribute<KunpengBindingType> bindingTypeAttribute;
  private static Attribute<String> versionTagAttribute;

  public KunpengCalledDecisionImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(KunpengCalledDecision.class, KunpengConstants.ELEMENT_CALLED_DECISION)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengCalledDecisionImpl::new);

    decisionIdAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_DECISION_ID)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .required()
            .build();

    resultVariableAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_RESULT_VARIABLE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .required()
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

  @Override
  public String getDecisionId() {
    return decisionIdAttribute.getValue(this);
  }

  @Override
  public void setDecisionId(final String decisionId) {
    decisionIdAttribute.setValue(this, decisionId);
  }

  @Override
  public String getResultVariable() {
    return resultVariableAttribute.getValue(this);
  }

  @Override
  public void setResultVariable(final String resultVariable) {
    resultVariableAttribute.setValue(this, resultVariable);
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
}
