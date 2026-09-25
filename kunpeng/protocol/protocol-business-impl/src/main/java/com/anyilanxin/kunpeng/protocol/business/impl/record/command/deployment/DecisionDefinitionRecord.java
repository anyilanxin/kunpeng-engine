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

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;

import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.DecisionDefinitionRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.BinaryProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 决策定义记录
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DecisionDefinitionRecord extends UnifiedRecordValue<DecisionDefinitionRecord>
    implements DecisionDefinitionRecordValue {
  // structpack-ids[DecisionDefinitionRecord]: 1,2,3,4,5,6,7,8,9,10,11,12,13,14
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
  private final StringProperty versionTagProp = new StringProperty(12, VERSION_TAG, "");
  private final IntegerProperty historyTimeToLiveProp =
      new IntegerProperty(7, "HISTORY_TIME_TO_LIVE", -1);
  private final LongProperty deploymentIdProp = new LongProperty(13, DEPLOYMENT_ID, -1);
  private final LongProperty resourceDefinitionIdProp =
      new LongProperty(8, "RESOURCE_DEFINITION_ID", -1);
  private final StringProperty resourceDefinitionNameProp =
      new StringProperty(9, "RESOURCE_DEFINITION_NAME", "");
  private final BinaryProperty checksumProp =
      new BinaryProperty(10, "CHECKSUM", new UnsafeBuffer());
  private final BinaryProperty resourceProp =
      new BinaryProperty(11, "RESOURCE", new UnsafeBuffer());
  private final StringProperty tenantIdProp =
      new StringProperty(14, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public DecisionDefinitionRecord() {
    super(14);
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
        .declareProperty(checksumProp)
        .declareProperty(resourceProp)
        .declareProperty(tenantIdProp);
  }

  @Override
  public long getDecisionDefinitionId() {
    return decisionDefinitionIdProp.getValue();
  }

  public DecisionDefinitionRecord setDecisionDefinitionId(final long decisionDefinitionId) {
    decisionDefinitionIdProp.setValue(decisionDefinitionId);
    return this;
  }

  @Override
  public String getDecisionDefinitionKey() {
    return bufferAsString(decisionDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getDecisionDefinitionKeyBuffer() {
    return decisionDefinitionKeyProp.getValue();
  }

  public DecisionDefinitionRecord setDecisionDefinitionKey(final String decisionDefinitionKey) {
    if (decisionDefinitionKey != null) {
      decisionDefinitionKeyProp.setValue(wrapString(decisionDefinitionKey));
    }
    return this;
  }

  public DecisionDefinitionRecord setDecisionDefinitionKey(
      final DirectBuffer decisionDefinitionKey) {
    decisionDefinitionKeyProp.setValue(decisionDefinitionKey);
    return this;
  }

  @Override
  public String getDecisionDefinitionName() {
    return bufferAsString(decisionDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getDecisionDefinitionNameBuffer() {
    return decisionDefinitionNameProp.getValue();
  }

  public DecisionDefinitionRecord setDecisionDefinitionName(final String decisionDefinitionName) {
    if (decisionDefinitionName != null) {
      decisionDefinitionNameProp.setValue(wrapString(decisionDefinitionName));
    }
    return this;
  }

  public DecisionDefinitionRecord setDecisionDefinitionName(
      final DirectBuffer decisionDefinitionName) {
    decisionDefinitionNameProp.setValue(decisionDefinitionName);
    return this;
  }

  @Override
  public int getDecisionDefinitionVersion() {
    return decisionDefinitionVersionProp.getValue();
  }

  public DecisionDefinitionRecord setDecisionDefinitionVersion(
      final int decisionDefinitionVersion) {
    decisionDefinitionVersionProp.setValue(decisionDefinitionVersion);
    return this;
  }

  @Override
  public long getDecisionRequirementDefinitionId() {
    return decisionRequirementDefinitionIdProp.getValue();
  }

  public DecisionDefinitionRecord setDecisionRequirementDefinitionId(
      final long decisionRequirementDefinitionId) {
    decisionRequirementDefinitionIdProp.setValue(decisionRequirementDefinitionId);
    return this;
  }

  @Override
  public String getDecisionRequirementDefinitionKey() {
    return bufferAsString(decisionRequirementDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getDecisionRequirementDefinitionKeyBuffer() {
    return decisionRequirementDefinitionKeyProp.getValue();
  }

  public DecisionDefinitionRecord setDecisionRequirementDefinitionKey(
      final String decisionRequirementDefinitionKey) {
    if (decisionRequirementDefinitionKey != null) {
      decisionRequirementDefinitionKeyProp.setValue(wrapString(decisionRequirementDefinitionKey));
    }
    return this;
  }

  public DecisionDefinitionRecord setDecisionRequirementDefinitionKey(
      final DirectBuffer decisionRequirementDefinitionKey) {
    decisionRequirementDefinitionKeyProp.setValue(decisionRequirementDefinitionKey);
    return this;
  }

  @Override
  public String getVersionTag() {
    return bufferAsString(versionTagProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getVersionTagBuffer() {
    return versionTagProp.getValue();
  }

  public DecisionDefinitionRecord setVersionTag(final String versionTag) {
    if (versionTag != null) {
      versionTagProp.setValue(wrapString(versionTag));
    }
    return this;
  }

  public DecisionDefinitionRecord setVersionTag(final DirectBuffer versionTag) {
    versionTagProp.setValue(versionTag);
    return this;
  }

  @Override
  public int getHistoryTimeToLive() {
    return historyTimeToLiveProp.getValue();
  }

  public DecisionDefinitionRecord setHistoryTimeToLive(final int historyTimeToLive) {
    historyTimeToLiveProp.setValue(historyTimeToLive);
    return this;
  }

  @Override
  public long getDeploymentId() {
    return deploymentIdProp.getValue();
  }

  public DecisionDefinitionRecord setDeploymentId(final long deploymentId) {
    deploymentIdProp.setValue(deploymentId);
    return this;
  }

  @Override
  public long getResourceDefinitionId() {
    return resourceDefinitionIdProp.getValue();
  }

  public DecisionDefinitionRecord setResourceDefinitionId(final long resourceId) {
    resourceDefinitionIdProp.setValue(resourceId);
    return this;
  }

  @Override
  public String getResourceDefinitionName() {
    return bufferAsString(resourceDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getResourceDefinitionNameBuffer() {
    return resourceDefinitionNameProp.getValue();
  }

  public DecisionDefinitionRecord setResourceDefinitionName(final String resourceName) {
    if (resourceName != null) {
      resourceDefinitionNameProp.setValue(wrapString(resourceName));
    }
    return this;
  }

  public DecisionDefinitionRecord setResourceDefinitionName(final DirectBuffer resourceName) {
    resourceDefinitionNameProp.setValue(resourceName);
    return this;
  }

  @Override
  public byte[] getChecksum() {
    return bufferAsArray(checksumProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getChecksumBuffer() {
    return checksumProp.getValue();
  }

  public DecisionDefinitionRecord setChecksum(
      final DirectBuffer checksum, final int offset, final int length) {
    checksumProp.setValue(checksum, offset, length);
    return this;
  }

  public DecisionDefinitionRecord setChecksum(final byte[] checksum) {
    checksumProp.setValue(BufferUtil.wrapArray(checksum));
    return this;
  }

  @Override
  public byte[] getResource() {
    return bufferAsArray(resourceProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getResourceBuffer() {
    return resourceProp.getValue();
  }

  public DecisionDefinitionRecord setResource(
      final DirectBuffer resource, final int offset, final int length) {
    resourceProp.setValue(resource, offset, length);
    return this;
  }

  public DecisionDefinitionRecord setResource(final byte[] resource) {
    resourceProp.setValue(BufferUtil.wrapArray(resource));
    return this;
  }

  @Override
  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public DecisionDefinitionRecord setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public DecisionDefinitionRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  @Override
  protected DecisionDefinitionRecord newRecord() {
    return new DecisionDefinitionRecord();
  }
}
