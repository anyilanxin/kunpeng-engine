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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.deployment.create;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.DEPLOYMENT_ID;
import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.TENANT_ID;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DecisionDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DecisionRequirementDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ResourceDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.create.*;
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
 * 部署创建响应 Record：部署键与各定义元数据。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DeploymentCreateResponseRecord
    extends UnifiedRecordValue<DeploymentCreateResponseRecord>
    implements DeploymentCreateResponseRecordValue {
  private final LongProperty deploymentIdProp = new LongProperty(8, DEPLOYMENT_ID, -1);
  private final StringProperty deploymentNameProp = new StringProperty(1, "DEPLOYMENT_NAME", "");
  private final LongProperty deploymentTimeProp = new LongProperty(2, "DEPLOYMENT_TIME", -1);
  private final LongProperty activateProcessDefinitionsOnProp =
      new LongProperty(3, "ACTIVATE_PROCESS_DEFINITIONS_ON", -1);
  private final ArrayProperty<DeploymentResourceDefinitionResponseRecord> resourceDefinitionsProp =
      new ArrayProperty<>(
          4, "RESOURCES_DEFINITIONS", DeploymentResourceDefinitionResponseRecord::new);
  private final ArrayProperty<DeploymentProcessDefinitionResponseRecord> processDefinitionsProp =
      new ArrayProperty<>(5, "PROCESS_DEFINITIONS", DeploymentProcessDefinitionResponseRecord::new);
  private final ArrayProperty<DecisionRequirementDefinitionResponseRecord>
      decisionRequirementDefinitionsProp =
          new ArrayProperty<>(
              6,
              "DECISION_REQUIREMENT_DEFINITIONS",
              DecisionRequirementDefinitionResponseRecord::new);
  private final ArrayProperty<DeploymentDecisionDefinitionResponseRecord> decisionDefinitionsProp =
      new ArrayProperty<>(
          7, "DECISION_DEFINITIONS", DeploymentDecisionDefinitionResponseRecord::new);
  private final StringProperty tenantIdProp =
      new StringProperty(9, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public DeploymentCreateResponseRecord() {
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

  public DeploymentCreateResponseRecord setDeploymentId(final long deploymentId) {
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

  public DeploymentCreateResponseRecord setDeploymentName(final String deploymentName) {
    if (deploymentName != null) {
      deploymentNameProp.setValue(wrapString(deploymentName));
    }
    return this;
  }

  public DeploymentCreateResponseRecord setDeploymentName(final DirectBuffer deploymentName) {
    deploymentNameProp.setValue(deploymentName);
    return this;
  }

  @Override
  public long getDeploymentTime() {
    return deploymentTimeProp.getValue();
  }

  public DeploymentCreateResponseRecord setDeploymentTime(final long deploymentTime) {
    deploymentTimeProp.setValue(deploymentTime);
    return this;
  }

  @Override
  public long getActivateProcessDefinitionsOn() {
    return activateProcessDefinitionsOnProp.getValue();
  }

  public DeploymentCreateResponseRecord setActivateProcessDefinitionsOn(
      final long activateProcessDefinitionsOn) {
    activateProcessDefinitionsOnProp.setValue(activateProcessDefinitionsOn);
    return this;
  }

  /** 追加一个资源定义(从命令侧 {@link ResourceDefinitionRecord} 复制字段) */
  public DeploymentCreateResponseRecord addResourceDefinition(
      final ResourceDefinitionRecord record) {
    final DirectBuffer checksumBuffer = record.getChecksumBuffer();
    resourceDefinitionsProp
        .add()
        .setResourceDefinitionId(record.getResourceDefinitionId())
        .setResourceDefinitionName(record.getResourceDefinitionNameBuffer())
        .setDefinitionVersion(record.getDefinitionVersion())
        .setResourceType(record.getResourceType())
        .setChecksum(checksumBuffer, 0, checksumBuffer.capacity())
        .setDeploymentId(record.getDeploymentId());
    return this;
  }

  @Override
  public List<DeploymentResourceDefinitionResponseRecordValue> getResourceDefinitions() {
    final List<DeploymentResourceDefinitionResponseRecordValue> list =
        new ArrayList<>(resourceDefinitionsProp.size());
    for (final DeploymentResourceDefinitionResponseRecord record : resourceDefinitionsProp) {
      list.add(record);
    }
    return list;
  }

  /** 追加一个流程定义(从命令侧 {@link ProcessDefinitionRecord} 复制字段) */
  public DeploymentCreateResponseRecord addProcessDefinition(final ProcessDefinitionRecord record) {
    final DirectBuffer checksumBuffer = record.getChecksumBuffer();
    processDefinitionsProp
        .add()
        .setProcessDefinitionId(record.getProcessDefinitionId())
        .setProcessDefinitionName(record.getProcessDefinitionNameBuffer())
        .setProcessDefinitionKey(record.getProcessDefinitionKeyBuffer())
        .setProcessDefinitionVersion(record.getProcessDefinitionVersion())
        .setSuspension(record.isSuspension())
        .setStartable(record.isStartable())
        .setHistoryTimeToLive(record.getHistoryTimeToLive())
        .setCandidateStarterGroups(record.getCandidateStarterGroups())
        .setCandidateStarterUsers(record.getCandidateStarterUsers())
        .setVersionTag(record.getVersionTagBuffer())
        .setResourceDefinitionId(record.getResourceDefinitionId())
        .setChecksum(checksumBuffer, 0, checksumBuffer.capacity());
    return this;
  }

  @Override
  public List<DeploymentProcessDefinitionResponseRecordValue> getProcessDefinitions() {
    final List<DeploymentProcessDefinitionResponseRecordValue> list =
        new ArrayList<>(processDefinitionsProp.size());
    for (final DeploymentProcessDefinitionResponseRecord record : processDefinitionsProp) {
      list.add(record);
    }
    return list;
  }

  /** 追加一个决策需求定义(从命令侧 {@link DecisionRequirementDefinitionRecord} 复制字段) */
  public DeploymentCreateResponseRecord addDecisionRequirementDefinition(
      final DecisionRequirementDefinitionRecord record) {
    final DirectBuffer checksumBuffer = record.getChecksumBuffer();
    decisionRequirementDefinitionsProp
        .add()
        .setDecisionRequirementDefinitionId(record.getDecisionRequirementDefinitionId())
        .setDecisionRequirementDefinitionKey(record.getDecisionRequirementDefinitionKeyBuffer())
        .setDecisionRequirementDefinitionName(record.getDecisionRequirementDefinitionNameBuffer())
        .setDecisionRequirementsVersion(record.getDecisionRequirementsVersion())
        .setResourceDefinitionId(record.getResourceDefinitionId())
        .setChecksum(checksumBuffer, 0, checksumBuffer.capacity());
    return this;
  }

  @Override
  public List<DecisionRequirementDefinitionResponseRecordValue>
      getDecisionRequirementDefinitions() {
    final List<DecisionRequirementDefinitionResponseRecordValue> list =
        new ArrayList<>(decisionRequirementDefinitionsProp.size());
    for (final DecisionRequirementDefinitionResponseRecord record :
        decisionRequirementDefinitionsProp) {
      list.add(record);
    }
    return list;
  }

  /** 追加一个决策定义(从命令侧 {@link DecisionDefinitionRecord} 复制字段) */
  public DeploymentCreateResponseRecord addDecisionDefinition(
      final DecisionDefinitionRecord record) {
    final DirectBuffer checksumBuffer = record.getChecksumBuffer();
    decisionDefinitionsProp
        .add()
        .setDecisionDefinitionId(record.getDecisionDefinitionId())
        .setDecisionDefinitionKey(record.getDecisionDefinitionKeyBuffer())
        .setDecisionDefinitionName(record.getDecisionDefinitionNameBuffer())
        .setDecisionDefinitionVersion(record.getDecisionDefinitionVersion())
        .setDecisionRequirementDefinitionId(record.getDecisionRequirementDefinitionId())
        .setDecisionRequirementDefinitionKey(record.getDecisionRequirementDefinitionKeyBuffer())
        .setVersionTag(record.getVersionTagBuffer())
        .setHistoryTimeToLive(record.getHistoryTimeToLive())
        .setResourceDefinitionId(record.getResourceDefinitionId())
        .setChecksum(checksumBuffer, 0, checksumBuffer.capacity());
    return this;
  }

  @Override
  public List<DeploymentDecisionDefinitionResponseRecordValue> getDecisionDefinitions() {
    final List<DeploymentDecisionDefinitionResponseRecordValue> list =
        new ArrayList<>(decisionDefinitionsProp.size());
    for (final DeploymentDecisionDefinitionResponseRecord record : decisionDefinitionsProp) {
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

  public DeploymentCreateResponseRecord setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public DeploymentCreateResponseRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  @Override
  protected DeploymentCreateResponseRecord newRecord() {
    return new DeploymentCreateResponseRecord();
  }
}
