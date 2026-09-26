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

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.create.DecisionRequirementDefinitionResponseRecordValue;
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
 * 部署返回的决策需求定义（DRD）信息 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DecisionRequirementDefinitionResponseRecord
    extends UnifiedRecordValue<DecisionRequirementDefinitionResponseRecord>
    implements DecisionRequirementDefinitionResponseRecordValue {
  private final LongProperty decisionRequirementDefinitionIdProp =
      new LongProperty(1, "DECISION_REQUIREMENT_DEFINITION_ID", -1);
  private final StringProperty decisionRequirementDefinitionKeyProp =
      new StringProperty(2, "DECISION_REQUIREMENT_DEFINITION_KEY", "");
  private final StringProperty decisionRequirementDefinitionNameProp =
      new StringProperty(3, "DECISION_REQUIREMENT_DEFINITION_NAME", "");
  private final IntegerProperty decisionRequirementsVersionProp =
      new IntegerProperty(4, "DECISION_REQUIREMENTS_VERSION", 0);
  private final LongProperty resourceDefinitionIdProp =
      new LongProperty(5, "RESOURCE_DEFINITION_ID", -1);
  private final StringProperty resourceDefinitionNameProp =
      new StringProperty(6, "RESOURCE_DEFINITION_NAME", "");
  private final BinaryProperty checksumProp = new BinaryProperty(7, "CHECKSUM", new UnsafeBuffer());

  public DecisionRequirementDefinitionResponseRecord() {
    super(7);
    declareProperty(decisionRequirementDefinitionIdProp)
        .declareProperty(decisionRequirementDefinitionKeyProp)
        .declareProperty(decisionRequirementDefinitionNameProp)
        .declareProperty(decisionRequirementsVersionProp)
        .declareProperty(resourceDefinitionIdProp)
        .declareProperty(resourceDefinitionNameProp)
        .declareProperty(checksumProp);
  }

  @Override
  public long getDecisionRequirementDefinitionId() {
    return decisionRequirementDefinitionIdProp.getValue();
  }

  public DecisionRequirementDefinitionResponseRecord setDecisionRequirementDefinitionId(
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

  public DecisionRequirementDefinitionResponseRecord setDecisionRequirementDefinitionKey(
      final String decisionRequirementDefinitionKey) {
    if (decisionRequirementDefinitionKey != null) {
      decisionRequirementDefinitionKeyProp.setValue(wrapString(decisionRequirementDefinitionKey));
    }
    return this;
  }

  public DecisionRequirementDefinitionResponseRecord setDecisionRequirementDefinitionKey(
      final DirectBuffer decisionRequirementDefinitionKey) {
    decisionRequirementDefinitionKeyProp.setValue(decisionRequirementDefinitionKey);
    return this;
  }

  @Override
  public String getDecisionRequirementDefinitionName() {
    return bufferAsString(decisionRequirementDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getDecisionRequirementDefinitionNameBuffer() {
    return decisionRequirementDefinitionNameProp.getValue();
  }

  public DecisionRequirementDefinitionResponseRecord setDecisionRequirementDefinitionName(
      final String decisionRequirementDefinitionName) {
    if (decisionRequirementDefinitionName != null) {
      decisionRequirementDefinitionNameProp.setValue(wrapString(decisionRequirementDefinitionName));
    }
    return this;
  }

  public DecisionRequirementDefinitionResponseRecord setDecisionRequirementDefinitionName(
      final DirectBuffer decisionRequirementDefinitionName) {
    decisionRequirementDefinitionNameProp.setValue(decisionRequirementDefinitionName);
    return this;
  }

  @Override
  public int getDecisionRequirementsVersion() {
    return decisionRequirementsVersionProp.getValue();
  }

  public DecisionRequirementDefinitionResponseRecord setDecisionRequirementsVersion(
      final int decisionRequirementsVersion) {
    decisionRequirementsVersionProp.setValue(decisionRequirementsVersion);
    return this;
  }

  @Override
  public long getResourceDefinitionId() {
    return resourceDefinitionIdProp.getValue();
  }

  public DecisionRequirementDefinitionResponseRecord setResourceDefinitionId(
      final long resourceId) {
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

  public DecisionRequirementDefinitionResponseRecord setResourceDefinitionName(
      final String resourceName) {
    if (resourceName != null) {
      resourceDefinitionNameProp.setValue(wrapString(resourceName));
    }
    return this;
  }

  public DecisionRequirementDefinitionResponseRecord setResourceDefinitionName(
      final DirectBuffer resourceName) {
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

  public DecisionRequirementDefinitionResponseRecord setChecksum(
      final DirectBuffer checksum, final int offset, final int length) {
    checksumProp.setValue(checksum, offset, length);
    return this;
  }

  public DecisionRequirementDefinitionResponseRecord setChecksum(final byte[] checksum) {
    checksumProp.setValue(BufferUtil.wrapArray(checksum));
    return this;
  }

  @Override
  protected DecisionRequirementDefinitionResponseRecord newRecord() {
    return new DecisionRequirementDefinitionResponseRecord();
  }
}
