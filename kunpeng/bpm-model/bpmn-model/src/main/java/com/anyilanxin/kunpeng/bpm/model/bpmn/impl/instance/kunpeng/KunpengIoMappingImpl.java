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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengInput;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengIoMapping;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengOutput;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import java.util.Collection;

public class KunpengIoMappingImpl extends BpmnModelElementInstanceImpl implements KunpengIoMapping {

  protected static ChildElementCollection<KunpengInput> inputCollection;
  protected static ChildElementCollection<KunpengOutput> outputCollection;

  public KunpengIoMappingImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Collection<KunpengInput> getInputs() {
    return inputCollection.get(this);
  }

  @Override
  public Collection<KunpengOutput> getOutputs() {
    return outputCollection.get(this);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(KunpengIoMapping.class, KunpengConstants.ELEMENT_IO_MAPPING)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengIoMappingImpl::new);

    inputCollection = typeBuilder.sequence().elementCollection(KunpengInput.class).build();
    outputCollection = typeBuilder.sequence().elementCollection(KunpengOutput.class).build();

    typeBuilder.build();
  }
}
