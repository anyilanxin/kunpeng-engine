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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengAssignmentDefinition;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public final class KunpengAssignmentDefinitionImpl extends BpmnModelElementInstanceImpl
    implements KunpengAssignmentDefinition {

  private static Attribute<String> assigneeAttribute;
  private static Attribute<String> candidateGroupsAttribute;
  private static Attribute<String> candidateUsersAttribute;

  public KunpengAssignmentDefinitionImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(
                KunpengAssignmentDefinition.class, KunpengConstants.ELEMENT_ASSIGNMENT_DEFINITION)
            .namespaceUri(BpmnModelConstants.KUNPENG_NS)
            .instanceProvider(KunpengAssignmentDefinitionImpl::new);

    assigneeAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_ASSIGNEE)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    candidateGroupsAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_CANDIDATE_GROUPS)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    candidateUsersAttribute =
        typeBuilder
            .stringAttribute(KunpengConstants.ATTRIBUTE_CANDIDATE_USERS)
            .namespace(BpmnModelConstants.KUNPENG_NS)
            .build();

    typeBuilder.build();
  }

  @Override
  public String getAssignee() {
    return assigneeAttribute.getValue(this);
  }

  @Override
  public void setAssignee(final String assignee) {
    assigneeAttribute.setValue(this, assignee);
  }

  @Override
  public String getCandidateGroups() {
    return candidateGroupsAttribute.getValue(this);
  }

  @Override
  public void setCandidateGroups(final String candidateGroups) {
    candidateGroupsAttribute.setValue(this, candidateGroups);
  }

  @Override
  public String getCandidateUsers() {
    return candidateUsersAttribute.getValue(this);
  }

  @Override
  public void setCandidateUsers(final String candidateUsers) {
    candidateUsersAttribute.setValue(this, candidateUsers);
  }
}
