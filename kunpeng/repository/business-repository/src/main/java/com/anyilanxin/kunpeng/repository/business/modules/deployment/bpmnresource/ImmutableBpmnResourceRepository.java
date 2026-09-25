/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DeploymentRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.repository.business.ResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;

/**
 * BPMN 资源域只读仓储接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ImmutableBpmnResourceRepository extends ResourceDataSplit {
  ProcessDefinitionRecord get(final long key);

  void get(final long key, DeploymentRecord record);

  ProcessDefinitionRecord get(final String processDefinitionKey, final String tenantId);

  ProcessDefinitionRecord get(
      final String processDefinitionKey, final String tenantId, final int version);

  ProcessDefinitionRuntime getRuntime(
      final String processDefinitionKey, final String tenantId, final int version);

  ProcessDefinitionRuntime getRuntimeByDeployment(
      final long deploymentId, final String processDefinitionKey);

  ProcessDefinitionRuntime getRuntime(final long processDefinitionId, final String tenantId);

  ProcessDefinitionRuntime getRuntime(final long processDefinitionId);

  int getVersion(final String processDefinitionKey, final String tenantId);
}
