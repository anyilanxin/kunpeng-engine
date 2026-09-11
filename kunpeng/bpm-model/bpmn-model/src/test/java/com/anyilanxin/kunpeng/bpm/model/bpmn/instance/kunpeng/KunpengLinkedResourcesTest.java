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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExtensionElements;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class KunpengLinkedResourcesTest extends BpmnModelElementInstanceTest {

    @Override
    public TypeAssumption getTypeAssumption() {
      return new TypeAssumption(BpmnModelConstants.KUNPENG_NS, false);
    }

    @Override
    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return Arrays.asList(
          new ChildElementAssumption(BpmnModelConstants.KUNPENG_NS, KunpengLinkedResource.class));
    }

    @Override
    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Collections.emptyList();
    }

    @Test
    public void shouldReadLinkedResourcesElements() {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn.createExecutableProcess("process")
                        .startEvent()
                        .serviceTask(
                                "my_linked_resource",
                                t ->
                                  t.kunpengLinkedResources(
                                                l ->
                                                        l.resourceId("id")
                                                                .resourceType("RPA")
                                                          .bindingType(KunpengBindingType.deployment)
                                                                .versionTag("1v")
                                                                .linkName("my_link")))
                        .endEvent()
                        .done();

        final ModelElementInstance userTask = modelInstance.getModelElementById("my_linked_resource");

        // when
      final Collection<KunpengLinkedResource> linkedResources = getLinkedResources(userTask);

        // then
        assertThat(linkedResources)
                .extracting("resourceId", "resourceType", "bindingType", "versionTag", "linkName")
          .containsExactly(tuple("id", "RPA", KunpengBindingType.deployment, "1v", "my_link"));
    }

  private Collection<KunpengLinkedResource> getLinkedResources(
            final ModelElementInstance elementInstance) {
        return elementInstance
                .getUniqueChildElementByType(ExtensionElements.class)
          .getUniqueChildElementByType(KunpengLinkedResources.class)
          .getChildElementsByType(KunpengLinkedResource.class);
    }
}
