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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengOutput;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class KunpengOutputImpl extends BpmnModelElementInstanceImpl implements KunpengOutput {

  private static Attribute<String> sourceAttribute;
  private static Attribute<String> targetAttribute;

  public KunpengOutputImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getSource() {
    return sourceAttribute.getValue(this);
  }

  @Override
  public void setSource(final String source) {
    sourceAttribute.setValue(this, source);
  }

  @Override
  public String getTarget() {
    return targetAttribute.getValue(this);
  }

  @Override
  public void setTarget(final String target) {
    targetAttribute.setValue(this, target);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(KunpengOutput.class, KunpengConstants.ELEMENT_OUTPUT)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengOutputImpl::new);

    sourceAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_SOURCE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .required()
            .build();

    targetAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_TARGET)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .required()
            .build();

    typeBuilder.build();
  }
}
