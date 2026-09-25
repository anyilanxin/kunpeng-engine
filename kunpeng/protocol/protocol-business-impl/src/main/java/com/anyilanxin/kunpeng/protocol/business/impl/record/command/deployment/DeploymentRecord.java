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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.DEPLOYMENT_ID;
import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.TENANT_ID;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.*;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import org.agrona.DirectBuffer;

/**
 * 部署记录
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DeploymentRecord extends UnifiedRecordValue<DeploymentRecord>
    implements DeploymentRecordValue {
  // structpack-ids[DeploymentRecord]: 1,2,3,4,5,6,7,8,9
  private final LongProperty deploymentIdProp = new LongProperty(8, DEPLOYMENT_ID, -1);
  private final StringProperty deploymentNameProp = new StringProperty(1, "DEPLOYMENT_NAME", "");
  private final LongProperty deploymentTimeProp = new LongProperty(2, "DEPLOYMENT_TIME", -1);
  private final LongProperty activateProcessDefinitionsOnProp =
      new LongProperty(3, "ACTIVATE_PROCESS_DEFINITIONS_ON", -1);
  private final ArrayProperty<ResourceDefinitionRecord> resourceDefinitionsProp =
      new ArrayProperty<>(4, "RESOURCES_DEFINITIONS", ResourceDefinitionRecord::new);
  private final ArrayProperty<ProcessDefinitionRecord> processDefinitionsProp =
      new ArrayProperty<>(5, "PROCESS_DEFINITIONS", ProcessDefinitionRecord::new);
  private final ArrayProperty<DecisionRequirementDefinitionRecord>
      decisionRequirementDefinitionsProp =
          new ArrayProperty<>(
              6, "DECISION_REQUIREMENT_DEFINITIONS", DecisionRequirementDefinitionRecord::new);
  private final ArrayProperty<DecisionDefinitionRecord> decisionDefinitionsProp =
      new ArrayProperty<>(7, "DECISION_DEFINITIONS", DecisionDefinitionRecord::new);
  private final StringProperty tenantIdProp =
      new StringProperty(9, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public DeploymentRecord() {
    super(9);
    declareProperty(deploymentIdProp)
        .declareProperty(deploymentNameProp)
        .declareProperty(deploymentTimeProp)
        .declareProperty(activateProcessDefinitionsOnProp)
        .declareProperty(resourceDefinitionsProp)
        .declareProperty(processDefinitionsProp)
        .declareProperty(decisionRequirementDefinitionsProp)
        .declareProperty(decisionDefinitionsProp)
        .declareProperty(tenantIdProp);
  }

  @Override
  public long getDeploymentId() {
    return deploymentIdProp.getValue();
  }

  public DeploymentRecord setDeploymentId(final long deploymentId) {
    deploymentIdProp.setValue(deploymentId);
    return this;
  }

  @Override
  public String getDeploymentName() {
    return bufferAsString(deploymentNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getDeploymentNameBuffer() {
    return deploymentNameProp.getValue();
  }

  public DeploymentRecord setDeploymentName(final String deploymentName) {
    if (deploymentName != null) {
      deploymentNameProp.setValue(wrapString(deploymentName));
    }
    return this;
  }

  public DeploymentRecord setDeploymentName(final DirectBuffer deploymentName) {
    deploymentNameProp.setValue(deploymentName);
    return this;
  }

  @Override
  public long getDeploymentTime() {
    return deploymentTimeProp.getValue();
  }

  public DeploymentRecord setDeploymentTime(final long deploymentTime) {
    deploymentTimeProp.setValue(deploymentTime);
    return this;
  }

  @Override
  public long getActivateProcessDefinitionsOn() {
    return activateProcessDefinitionsOnProp.getValue();
  }

  public DeploymentRecord setActivateProcessDefinitionsOn(final long activateProcessDefinitionsOn) {
    activateProcessDefinitionsOnProp.setValue(activateProcessDefinitionsOn);
    return this;
  }

  /** 资源列表(原始数组访问,引擎处理器使用) */
  public ArrayProperty<ResourceDefinitionRecord> resourceDefinitions() {
    return resourceDefinitionsProp;
  }

  @Override
  public List<ResourceDefinitionValue> getResourceDefinitions() {
    final List<ResourceDefinitionValue> list = new ArrayList<>(processDefinitionsProp.size());
    for (final ResourceDefinitionRecord record : resourceDefinitionsProp) {
      list.add(record);
    }
    return list;
  }

  /** 流程定义列表(原始数组访问,引擎处理器使用) */
  public ArrayProperty<ProcessDefinitionRecord> processDefinitions() {
    return processDefinitionsProp;
  }

  @Override
  public List<ProcessDefinitionRecordValue> getProcessDefinitions() {
    final List<ProcessDefinitionRecordValue> list = new ArrayList<>(processDefinitionsProp.size());
    for (final ProcessDefinitionRecord record : processDefinitionsProp) {
      list.add(record);
    }
    return list;
  }

  /** 决策需求定义列表(原始数组访问,引擎处理器使用) */
  public ArrayProperty<DecisionRequirementDefinitionRecord> decisionRequirementDefinitions() {
    return decisionRequirementDefinitionsProp;
  }

  @Override
  public List<DecisionRequirementDefinitionRecordValue> getDecisionRequirementDefinitions() {
    final List<DecisionRequirementDefinitionRecordValue> list =
        new ArrayList<>(decisionRequirementDefinitionsProp.size());
    for (final DecisionRequirementDefinitionRecord record : decisionRequirementDefinitionsProp) {
      list.add(record);
    }
    return list;
  }

  /** 决策定义列表(原始数组访问,引擎处理器使用) */
  public ArrayProperty<DecisionDefinitionRecord> decisionDefinitions() {
    return decisionDefinitionsProp;
  }

  @Override
  public List<DecisionDefinitionRecordValue> getDecisionDefinitions() {
    final List<DecisionDefinitionRecordValue> list =
        new ArrayList<>(decisionDefinitionsProp.size());
    for (final DecisionDefinitionRecord record : decisionDefinitionsProp) {
      list.add(record);
    }
    return list;
  }

  @Override
  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public DeploymentRecord setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public DeploymentRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  @Override
  protected DeploymentRecord newRecord() {
    return new DeploymentRecord();
  }
}
