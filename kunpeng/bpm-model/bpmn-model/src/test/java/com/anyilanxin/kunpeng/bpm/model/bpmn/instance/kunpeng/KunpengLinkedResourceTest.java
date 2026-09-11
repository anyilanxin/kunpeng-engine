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
package com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstanceTest;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ServiceTask;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class KunpengLinkedResourceTest extends BpmnModelElementInstanceTest {

    @Override
    public TypeAssumption getTypeAssumption() {
      return new TypeAssumption(BpmnModelConstants.KUNPENG_NS, false);
    }

    @Override
    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Collections.emptyList();
    }

    @Override
    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Arrays.asList(
          new AttributeAssumption(BpmnModelConstants.KUNPENG_NS, "resourceId", false, false),
          new AttributeAssumption(BpmnModelConstants.KUNPENG_NS, "versionTag", false, false),
          new AttributeAssumption(BpmnModelConstants.KUNPENG_NS, "resourceType", false, false),
                new AttributeAssumption(
                  BpmnModelConstants.KUNPENG_NS, "bindingType", false, false, KunpengBindingType.latest),
          new AttributeAssumption(BpmnModelConstants.KUNPENG_NS, "linkName", false, false));
    }

    @Test
    public void shouldReadValidBindingTypeFromXml() {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn.createExecutableProcess()
                        .startEvent()
                        .serviceTask(
                                "task",
                          t -> t.kunpengLinkedResources(l -> l.bindingType(KunpengBindingType.deployment)))
                        .done();
        final String modelXml = Bpmn.convertToString(modelInstance);

        // when
        final ServiceTask serviceTask =
                Bpmn.readModelFromStream(new ByteArrayInputStream(modelXml.getBytes()))
                        .getModelElementById("task");
      final KunpengLinkedResources linkedResource =
        serviceTask.getSingleExtensionElement(KunpengLinkedResources.class);

        // then
      final Optional<KunpengLinkedResource> first =
                linkedResource.getLinkedResources().stream().findFirst();
        assertThat(first).isPresent();
      assertThat(first.get().getBindingType()).isEqualTo(KunpengBindingType.deployment);
    }
}
