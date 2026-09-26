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

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DecisionDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import org.agrona.DirectBuffer;

/**
 * 决策定义 Entity：决策定义 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DecisionDefinitionEntity extends UnpackedObject implements StoreValue {
  private final LongProperty decisionDefinitionIdProp =
      new LongProperty(1, "DECISION_DEFINITION_ID", -1);
  private final StringProperty decisionDefinitionKeyProp =
      new StringProperty(2, "DECISION_DEFINITION_KEY", "");
  private final StringProperty decisionDefinitionNameProp =
      new StringProperty(3, "DECISION_DEFINITION_NAME", "");
  private final IntegerProperty decisionDefinitionVersionProp =
      new IntegerProperty(4, "DECISION_DEFINITION_VERSION", 0);
  private final LongProperty decisionRequirementDefinitionIdProp =
      new LongProperty(5, "DECISION_REQUIREMENT_DEFINITION_ID", -1);
  private final StringProperty decisionRequirementDefinitionKeyProp =
      new StringProperty(6, "DECISION_REQUIREMENT_DEFINITION_KEY", "");
  private final StringProperty versionTagProp = new StringProperty(7, VERSION_TAG, "");
  private final IntegerProperty historyTimeToLiveProp =
      new IntegerProperty(8, "HISTORY_TIME_TO_LIVE", -1);
  private final LongProperty deploymentIdProp = new LongProperty(9, DEPLOYMENT_ID, -1);
  private final LongProperty resourceDefinitionIdProp =
      new LongProperty(10, "RESOURCE_DEFINITION_ID", -1);
  private final StringProperty resourceDefinitionNameProp =
      new StringProperty(11, "RESOURCE_DEFINITION_NAME", "");
  private final StringProperty tenantIdProp =
      new StringProperty(12, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public DecisionDefinitionEntity() {
    super(12);
    declareProperty(decisionDefinitionIdProp)
        .declareProperty(decisionDefinitionKeyProp)
        .declareProperty(decisionDefinitionNameProp)
        .declareProperty(decisionDefinitionVersionProp)
        .declareProperty(decisionRequirementDefinitionIdProp)
        .declareProperty(decisionRequirementDefinitionKeyProp)
        .declareProperty(versionTagProp)
        .declareProperty(historyTimeToLiveProp)
        .declareProperty(deploymentIdProp)
        .declareProperty(resourceDefinitionIdProp)
        .declareProperty(resourceDefinitionNameProp)
        .declareProperty(tenantIdProp);
  }

  public void wrap(final DecisionDefinitionRecord record) {
    setDecisionDefinitionId(record.getDecisionDefinitionId())
        .setDecisionDefinitionKey(record.getDecisionDefinitionKeyBuffer())
        .setDecisionDefinitionName(record.getDecisionDefinitionNameBuffer())
        .setDecisionDefinitionVersion(record.getDecisionDefinitionVersion())
        .setDecisionRequirementDefinitionId(record.getDecisionRequirementDefinitionId())
        .setDecisionRequirementDefinitionKey(record.getDecisionRequirementDefinitionKeyBuffer())
        .setVersionTag(record.getVersionTagBuffer())
        .setHistoryTimeToLive(record.getHistoryTimeToLive())
        .setDeploymentId(record.getDeploymentId())
        .setResourceDefinitionId(record.getResourceDefinitionId())
        .setResourceDefinitionName(record.getResourceDefinitionNameBuffer())
        .setTenantId(record.getTenantIdBuffer());
  }

  public DecisionDefinitionRecord unwrap(final DecisionDefinitionRecord record) {
    record.reset();
    return record
        .setDecisionDefinitionId(getDecisionDefinitionId())
        .setDecisionDefinitionKey(getDecisionDefinitionKeyBuffer())
        .setDecisionDefinitionName(getDecisionDefinitionNameBuffer())
        .setDecisionDefinitionVersion(getDecisionDefinitionVersion())
        .setDecisionRequirementDefinitionId(getDecisionRequirementDefinitionId())
        .setDecisionRequirementDefinitionKey(getDecisionRequirementDefinitionKeyBuffer())
        .setVersionTag(getVersionTagBuffer())
        .setHistoryTimeToLive(getHistoryTimeToLive())
        .setDeploymentId(getDeploymentId())
        .setResourceDefinitionId(getResourceDefinitionId())
        .setResourceDefinitionName(getResourceDefinitionNameBuffer())
        .setTenantId(getTenantIdBuffer());
  }

  public long getDecisionDefinitionId() {
    return decisionDefinitionIdProp.getValue();
  }

  public DecisionDefinitionEntity setDecisionDefinitionId(final long decisionDefinitionId) {
    decisionDefinitionIdProp.setValue(decisionDefinitionId);
    return this;
  }

  public String getDecisionDefinitionKey() {
    return bufferAsString(decisionDefinitionKeyProp.getValue());
  }

  public DirectBuffer getDecisionDefinitionKeyBuffer() {
    return decisionDefinitionKeyProp.getValue();
  }

  public DecisionDefinitionEntity setDecisionDefinitionKey(final String decisionDefinitionKey) {
    if (decisionDefinitionKey != null) {
      decisionDefinitionKeyProp.setValue(wrapString(decisionDefinitionKey));
    }
    return this;
  }

  public DecisionDefinitionEntity setDecisionDefinitionKey(
      final DirectBuffer decisionDefinitionKey) {
    decisionDefinitionKeyProp.setValue(decisionDefinitionKey);
    return this;
  }

  public String getDecisionDefinitionName() {
    return bufferAsString(decisionDefinitionNameProp.getValue());
  }

  public DirectBuffer getDecisionDefinitionNameBuffer() {
    return decisionDefinitionNameProp.getValue();
  }

  public DecisionDefinitionEntity setDecisionDefinitionName(final String decisionDefinitionName) {
    if (decisionDefinitionName != null) {
      decisionDefinitionNameProp.setValue(wrapString(decisionDefinitionName));
    }
    return this;
  }

  public DecisionDefinitionEntity setDecisionDefinitionName(
      final DirectBuffer decisionDefinitionName) {
    decisionDefinitionNameProp.setValue(decisionDefinitionName);
    return this;
  }

  public int getDecisionDefinitionVersion() {
    return decisionDefinitionVersionProp.getValue();
  }

  public DecisionDefinitionEntity setDecisionDefinitionVersion(
      final int decisionDefinitionVersion) {
    decisionDefinitionVersionProp.setValue(decisionDefinitionVersion);
    return this;
  }

  public long getDecisionRequirementDefinitionId() {
    return decisionRequirementDefinitionIdProp.getValue();
  }

  public DecisionDefinitionEntity setDecisionRequirementDefinitionId(
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

  public DecisionDefinitionEntity setDecisionRequirementDefinitionKey(
      final String decisionRequirementDefinitionKey) {
    if (decisionRequirementDefinitionKey != null) {
      decisionRequirementDefinitionKeyProp.setValue(wrapString(decisionRequirementDefinitionKey));
    }
    return this;
  }

  public DecisionDefinitionEntity setDecisionRequirementDefinitionKey(
      final DirectBuffer decisionRequirementDefinitionKey) {
    decisionRequirementDefinitionKeyProp.setValue(decisionRequirementDefinitionKey);
    return this;
  }

  public String getVersionTag() {
    return bufferAsString(versionTagProp.getValue());
  }

  public DirectBuffer getVersionTagBuffer() {
    return versionTagProp.getValue();
  }

  public DecisionDefinitionEntity setVersionTag(final String versionTag) {
    if (versionTag != null) {
      versionTagProp.setValue(wrapString(versionTag));
    }
    return this;
  }

  public DecisionDefinitionEntity setVersionTag(final DirectBuffer versionTag) {
    versionTagProp.setValue(versionTag);
    return this;
  }

  public int getHistoryTimeToLive() {
    return historyTimeToLiveProp.getValue();
  }

  public DecisionDefinitionEntity setHistoryTimeToLive(final int historyTimeToLive) {
    historyTimeToLiveProp.setValue(historyTimeToLive);
    return this;
  }

  public long getDeploymentId() {
    return deploymentIdProp.getValue();
  }

  public DecisionDefinitionEntity setDeploymentId(final long deploymentId) {
    deploymentIdProp.setValue(deploymentId);
    return this;
  }

  public long getResourceDefinitionId() {
    return resourceDefinitionIdProp.getValue();
  }

  public DecisionDefinitionEntity setResourceDefinitionId(final long resourceDefinitionId) {
    resourceDefinitionIdProp.setValue(resourceDefinitionId);
    return this;
  }

  public String getResourceDefinitionName() {
    return bufferAsString(resourceDefinitionNameProp.getValue());
  }

  public DirectBuffer getResourceDefinitionNameBuffer() {
    return resourceDefinitionNameProp.getValue();
  }

  public DecisionDefinitionEntity setResourceDefinitionName(final String resourceDefinitionName) {
    if (resourceDefinitionName != null) {
      resourceDefinitionNameProp.setValue(wrapString(resourceDefinitionName));
    }
    return this;
  }

  public DecisionDefinitionEntity setResourceDefinitionName(
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

  public DecisionDefinitionEntity setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public DecisionDefinitionEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
