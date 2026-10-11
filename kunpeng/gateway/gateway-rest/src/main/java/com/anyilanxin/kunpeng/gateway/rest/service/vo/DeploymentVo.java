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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.rest.service.vo;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.create.DeploymentCreateResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.create.DeploymentProcessDefinitionResponseRecordValue;
import java.util.List;

/**
 * 部署结果。
 *
 * @param deploymentKey 部署 key
 * @param tenantId 租户
 * @param processDefinitions 本次部署的流程定义列表
 * @author zxuanhong
 * @since 2026.9.0
 */
public record DeploymentVo(
    long deploymentKey, String tenantId, List<ProcessDefinitionVo> processDefinitions) {

  /** 部署内的流程定义。 */
  public record ProcessDefinitionVo(
      long processDefinitionId, String processDefinitionKey, int version, String resourceName) {}

  public static DeploymentVo of(final DeploymentCreateResponseRecordValue value) {
    final List<ProcessDefinitionVo> processDefinitions =
        value.getProcessDefinitions().stream().map(DeploymentVo::toProcessDefinition).toList();
    return new DeploymentVo(value.getDeploymentId(), value.getTenantId(), processDefinitions);
  }

  private static ProcessDefinitionVo toProcessDefinition(
      final DeploymentProcessDefinitionResponseRecordValue value) {
    return new ProcessDefinitionVo(
        value.getProcessDefinitionId(),
        value.getProcessDefinitionKey(),
        value.getProcessDefinitionVersion(),
        value.getResourceDefinitionName());
  }
}
