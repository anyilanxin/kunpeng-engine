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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class KunpengVersionTagTest extends BpmnModelElementInstanceTest {

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
        return Collections.singletonList(
          new AttributeAssumption(BpmnModelConstants.KUNPENG_NS, "value", false, true));
    }

    @Test
    public void shouldReadVersionTagFromXml() {
        // given
        final BpmnModelInstance modelInstance =
                Bpmn.createExecutableProcess("process").versionTag("v1").startEvent().done();
        final String modelXml = Bpmn.convertToString(modelInstance);

        // when
        final Process process =
                Bpmn.readModelFromStream(new ByteArrayInputStream(modelXml.getBytes()))
                        .getModelElementById("process");
      final KunpengVersionTag versionTag = process.getSingleExtensionElement(KunpengVersionTag.class);

        // then
      assertThat(versionTag).isNotNull().extracting(KunpengVersionTag::getValue).isEqualTo("v1");
    }
}
