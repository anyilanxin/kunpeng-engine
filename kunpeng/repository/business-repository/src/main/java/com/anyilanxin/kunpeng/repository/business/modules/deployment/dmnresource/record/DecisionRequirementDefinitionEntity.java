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
package com.anyilanxin.kunpeng.repository.business.modules.deployment.dmnresource.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.DEPLOYMENT_ID;
import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.TENANT_ID;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DecisionRequirementDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import org.agrona.DirectBuffer;

/**
 * 决策需求定义 Entity：决策需求定义 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DecisionRequirementDefinitionEntity extends UnpackedObject implements StoreValue {
  private final LongProperty decisionRequirementDefinitionIdProp =
      new LongProperty(1, "DECISION_REQUIREMENT_DEFINITION_ID", -1);
  private final StringProperty decisionRequirementDefinitionKeyProp =
      new StringProperty(2, "DECISION_REQUIREMENT_DEFINITION_KEY", "");
  private final StringProperty decisionRequirementDefinitionNameProp =
      new StringProperty(3, "DECISION_REQUIREMENT_DEFINITION_NAME", "");
  private final IntegerProperty decisionRequirementsVersionProp =
      new IntegerProperty(4, "DECISION_REQUIREMENTS_VERSION", 0);
  private final LongProperty deploymentIdProp = new LongProperty(5, DEPLOYMENT_ID, -1);
  private final LongProperty resourceDefinitionIdProp =
      new LongProperty(6, "RESOURCE_DEFINITION_ID", -1);
  private final StringProperty resourceDefinitionNameProp =
      new StringProperty(7, "RESOURCE_DEFINITION_NAME", "");
  private final StringProperty tenantIdProp =
      new StringProperty(8, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public DecisionRequirementDefinitionEntity() {
    super(8);
    declareProperty(decisionRequirementDefinitionIdProp)
        .declareProperty(decisionRequirementDefinitionKeyProp)
        .declareProperty(decisionRequirementDefinitionNameProp)
        .declareProperty(decisionRequirementsVersionProp)
        .declareProperty(deploymentIdProp)
        .declareProperty(resourceDefinitionIdProp)
        .declareProperty(resourceDefinitionNameProp)
        .declareProperty(tenantIdProp);
  }

  public void wrap(final DecisionRequirementDefinitionRecord record) {
    setDecisionRequirementDefinitionId(record.getDecisionRequirementDefinitionId())
        .setDecisionRequirementDefinitionKey(record.getDecisionRequirementDefinitionKeyBuffer())
        .setDecisionRequirementDefinitionName(record.getDecisionRequirementDefinitionNameBuffer())
        .setDecisionRequirementsVersion(record.getDecisionRequirementsVersion())
        .setDeploymentId(record.getDeploymentId())
        .setResourceDefinitionId(record.getResourceDefinitionId())
        .setResourceDefinitionName(record.getResourceDefinitionNameBuffer())
        .setTenantId(record.getTenantIdBuffer());
  }

  public DecisionRequirementDefinitionRecord unwrap(
      final DecisionRequirementDefinitionRecord record) {
    record.reset();
    return record
        .setDecisionRequirementDefinitionId(getDecisionRequirementDefinitionId())
        .setDecisionRequirementDefinitionKey(getDecisionRequirementDefinitionKeyBuffer())
        .setDecisionRequirementDefinitionName(getDecisionRequirementDefinitionNameBuffer())
        .setDecisionRequirementsVersion(getDecisionRequirementsVersion())
        .setDeploymentId(getDeploymentId())
        .setResourceDefinitionId(getResourceDefinitionId())
        .setResourceDefinitionName(getResourceDefinitionNameBuffer())
        .setTenantId(getTenantIdBuffer());
  }

  public long getDecisionRequirementDefinitionId() {
    return decisionRequirementDefinitionIdProp.getValue();
  }

  public DecisionRequirementDefinitionEntity setDecisionRequirementDefinitionId(
      final long decisionRequirementDefinitionId) {
    decisionRequirementDefinitionIdProp.setValue(decisionRequirementDefinitionId);
    return this;
  }

  public String getDecisionRequirementDefinitionKey() {
    return bufferAsString(decisionRequirementDefinitionKeyProp.getValue());
  }

  public DirectBuffer getDecisionRequirementDefinitionKeyBuffer() {
    return decisionRequirementDefinitionKeyProp.getValue();
  }

  public DecisionRequirementDefinitionEntity setDecisionRequirementDefinitionKey(
      final String decisionRequirementDefinitionKey) {
    if (decisionRequirementDefinitionKey != null) {
      decisionRequirementDefinitionKeyProp.setValue(wrapString(decisionRequirementDefinitionKey));
    }
    return this;
  }

  public DecisionRequirementDefinitionEntity setDecisionRequirementDefinitionKey(
      final DirectBuffer decisionRequirementDefinitionKey) {
    decisionRequirementDefinitionKeyProp.setValue(decisionRequirementDefinitionKey);
    return this;
  }

  public String getDecisionRequirementDefinitionName() {
    return bufferAsString(decisionRequirementDefinitionNameProp.getValue());
  }

  public DirectBuffer getDecisionRequirementDefinitionNameBuffer() {
    return decisionRequirementDefinitionNameProp.getValue();
  }

  public DecisionRequirementDefinitionEntity setDecisionRequirementDefinitionName(
      final String decisionRequirementDefinitionName) {
    if (decisionRequirementDefinitionName != null) {
      decisionRequirementDefinitionNameProp.setValue(wrapString(decisionRequirementDefinitionName));
    }
    return this;
  }

  public DecisionRequirementDefinitionEntity setDecisionRequirementDefinitionName(
      final DirectBuffer decisionRequirementDefinitionName) {
    decisionRequirementDefinitionNameProp.setValue(decisionRequirementDefinitionName);
    return this;
  }

  public int getDecisionRequirementsVersion() {
    return decisionRequirementsVersionProp.getValue();
  }

  public DecisionRequirementDefinitionEntity setDecisionRequirementsVersion(
      final int decisionRequirementsVersion) {
    decisionRequirementsVersionProp.setValue(decisionRequirementsVersion);
    return this;
  }

  public long getDeploymentId() {
    return deploymentIdProp.getValue();
  }

  public DecisionRequirementDefinitionEntity setDeploymentId(final long deploymentId) {
    deploymentIdProp.setValue(deploymentId);
    return this;
  }

  public long getResourceDefinitionId() {
    return resourceDefinitionIdProp.getValue();
  }

  public DecisionRequirementDefinitionEntity setResourceDefinitionId(
      final long resourceDefinitionId) {
    resourceDefinitionIdProp.setValue(resourceDefinitionId);
    return this;
  }

  public String getResourceDefinitionName() {
    return bufferAsString(resourceDefinitionNameProp.getValue());
  }

  public DirectBuffer getResourceDefinitionNameBuffer() {
    return resourceDefinitionNameProp.getValue();
  }

  public DecisionRequirementDefinitionEntity setResourceDefinitionName(
      final String resourceDefinitionName) {
    if (resourceDefinitionName != null) {
      resourceDefinitionNameProp.setValue(wrapString(resourceDefinitionName));
    }
    return this;
  }

  public DecisionRequirementDefinitionEntity setResourceDefinitionName(
      final DirectBuffer resourceDefinitionName) {
    resourceDefinitionNameProp.setValue(resourceDefinitionName);
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public DecisionRequirementDefinitionEntity setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public DecisionRequirementDefinitionEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
