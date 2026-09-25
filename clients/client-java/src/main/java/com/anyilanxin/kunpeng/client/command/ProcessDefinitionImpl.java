/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.client.command;

import com.anyilanxin.kunpeng.gateway.grpc.service.DeploymentServiceOuterClass;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public final class ProcessDefinitionImpl implements ProcessDefinition {
  private final long processDefinitionId;
  private final String processDefinitionKey;
  private final int processDefinitionVersion;
  private final String resourceName;
  private final String tenantId;

  public ProcessDefinitionImpl(
      final DeploymentServiceOuterClass.ProcessDefinition processDefinition) {
    this(
        processDefinition.getProcessDefinitionId(),
        processDefinition.getProcessDefinitionKey(),
        processDefinition.getProcessDefinitionVersion(),
        processDefinition.getResourceName(),
        processDefinition.getTenantId());
  }

  public ProcessDefinitionImpl(
      final long processDefinitionId,
      final String processDefinitionKey,
      final int processDefinitionVersion,
      final String resourceName,
      final String tenantId) {
    this.processDefinitionId = processDefinitionId;
    this.processDefinitionKey = processDefinitionKey;
    this.processDefinitionVersion = processDefinitionVersion;
    this.resourceName = resourceName;
    this.tenantId = tenantId;
  }

  @Override
  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  @Override
  public int getProcessDefinitionVersion() {
    return processDefinitionVersion;
  }

  @Override
  public long getProcessDefinitionId() {
    return processDefinitionId;
  }

  @Override
  public String getResourceName() {
    return resourceName;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }
}
