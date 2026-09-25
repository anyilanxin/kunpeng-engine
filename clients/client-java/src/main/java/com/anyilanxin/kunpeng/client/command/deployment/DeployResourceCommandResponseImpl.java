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
package com.anyilanxin.kunpeng.client.command.deployment;

import com.anyilanxin.kunpeng.client.command.FormDefinition;
import com.anyilanxin.kunpeng.client.command.ProcessDefinition;
import com.anyilanxin.kunpeng.client.command.ProcessDefinitionImpl;
import com.anyilanxin.kunpeng.gateway.grpc.service.DeploymentServiceOuterClass;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class DeployResourceCommandResponseImpl implements DeployResourceCommandResponse {
  private final long deploymentId;
  private final String tenantId;
  private final List<ProcessDefinition> processDefinitions = new ArrayList<>();
  private final List<DecisionDefinition> decisions = new ArrayList<>();
  private final List<DecisionRequirementDefinition> decisionRequirements = new ArrayList<>();
  private final List<FormDefinition> forms = new ArrayList<>();

  public DeployResourceCommandResponseImpl(
      final DeploymentServiceOuterClass.DeployResourceResponse response) {
    deploymentId = response.getDeploymentId();
    tenantId = response.getTenantId();
    for (final DeploymentServiceOuterClass.ProcessDefinition processDefinition :
        response.getProcessDefinitionsList()) {
      processDefinitions.add(new ProcessDefinitionImpl(processDefinition));
    }
  }

  @Override
  public long getDeploymentId() {
    return deploymentId;
  }

  @Override
  public List<ProcessDefinition> getProcessDefinitions() {
    return processDefinitions;
  }

  @Override
  public List<DecisionDefinition> getDecisions() {
    return decisions;
  }

  @Override
  public List<DecisionRequirementDefinition> getDecisionRequirements() {
    return decisionRequirements;
  }

  @Override
  public List<FormDefinition> getForm() {
    return forms;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }
}
