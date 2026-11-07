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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengAddSign;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class KunpengAddSignImpl extends BpmnModelElementInstanceImpl implements KunpengAddSign {

  private static Attribute<Boolean> cascadeAttribute;
  private static Attribute<Integer> maxDepthAttribute;

  public KunpengAddSignImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final var typeBuilder =
        modelBuilder
            .defineType(KunpengAddSign.class, KunpengConstants.ELEMENT_ADD_SIGN)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengAddSignImpl::new);
    cascadeAttribute =
        typeBuilder
            .booleanAttribute(KunpengConstants.ATTRIBUTE_CASCADE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .defaultValue(Boolean.FALSE)
            .build();
    maxDepthAttribute =
        typeBuilder
            .integerAttribute(KunpengConstants.ATTRIBUTE_MAX_DEPTH)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .defaultValue(1)
            .build();
    typeBuilder.build();
  }

  @Override
  public Boolean getCascade() {
    return cascadeAttribute.getValue(this);
  }

  @Override
  public void setCascade(final Boolean cascade) {
    cascadeAttribute.setValue(this, cascade);
  }

  @Override
  public Integer getMaxDepth() {
    return maxDepthAttribute.getValue(this);
  }

  @Override
  public void setMaxDepth(final Integer maxDepth) {
    maxDepthAttribute.setValue(this, maxDepth);
  }
}
