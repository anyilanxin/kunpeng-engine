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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengScript;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class KunpengScriptImpl extends BpmnModelElementInstanceImpl implements KunpengScript {

  private static Attribute<String> expressionAttribute;
  private static Attribute<String> resultVariableAttribute;

  public KunpengScriptImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(KunpengScript.class, KunpengConstants.ELEMENT_SCRIPT)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengScriptImpl::new);

    expressionAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_EXPRESSION)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .required()
            .build();

    resultVariableAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_RESULT_VARIABLE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .required()
            .build();

    typeBuilder.build();
  }

  @Override
  public String getExpression() {
    return expressionAttribute.getValue(this);
  }

  @Override
  public void setExpression(final String expression) {
    expressionAttribute.setValue(this, expression);
  }

  @Override
  public String getResultVariable() {
    return resultVariableAttribute.getValue(this);
  }

  @Override
  public void setResultVariable(final String resultVariable) {
    resultVariableAttribute.setValue(this, resultVariable);
  }
}
