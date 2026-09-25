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

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;

import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ResourceType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.create.DeploymentResourceDefinitionResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.BinaryProperty;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 部署返回的资源定义信息 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DeploymentResourceDefinitionResponseRecord
    extends UnifiedRecordValue<DeploymentResourceDefinitionResponseRecord>
    implements DeploymentResourceDefinitionResponseRecordValue {
  // structpack-ids[DeploymentResourceDefinitionResponseRecord]: 1,2,3,4,5,6
  private final LongProperty resourceDefinitionIdProp =
      new LongProperty(1, "RESOURCE_DEFINITION_ID", -1);
  private final StringProperty resourceDefinitionNameProp =
      new StringProperty(2, "RESOURCE_DEFINITION_NAME", "");
  private final IntegerProperty definitionVersionProp =
      new IntegerProperty(3, "DEFINITION_VERSION", -1);
  private final EnumProperty<ResourceType> resourceTypeProp =
      new EnumProperty<>(4, "RESOURCE_TYPE", ResourceType.class, ResourceType.NULL_VAL);
  private final BinaryProperty checksumProp = new BinaryProperty(5, "CHECKSUM", new UnsafeBuffer());
  private final LongProperty deploymentIdProp = new LongProperty(6, DEPLOYMENT_ID, -1);

  public DeploymentResourceDefinitionResponseRecord() {
    super(6);
    declareProperty(resourceDefinitionIdProp)
        .declareProperty(resourceDefinitionNameProp)
        .declareProperty(definitionVersionProp)
        .declareProperty(resourceTypeProp)
        .declareProperty(checksumProp)
        .declareProperty(deploymentIdProp);
  }

  @Override
  public long getResourceDefinitionId() {
    return resourceDefinitionIdProp.getValue();
  }

  public DeploymentResourceDefinitionResponseRecord setResourceDefinitionId(final long resourceId) {
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

  public DeploymentResourceDefinitionResponseRecord setResourceDefinitionName(
      final String resourceName) {
    if (resourceName != null) {
      resourceDefinitionNameProp.setValue(wrapString(resourceName));
    }
    return this;
  }

  public DeploymentResourceDefinitionResponseRecord setResourceDefinitionName(
      final DirectBuffer resourceName) {
    resourceDefinitionNameProp.setValue(resourceName);
    return this;
  }

  @Override
  public int getDefinitionVersion() {
    return definitionVersionProp.getValue();
  }

  public DeploymentResourceDefinitionResponseRecord setDefinitionVersion(
      final int definitionVersion) {
    definitionVersionProp.setValue(definitionVersion);
    return this;
  }

  @Override
  public ResourceType getResourceType() {
    return resourceTypeProp.getValue();
  }

  public DeploymentResourceDefinitionResponseRecord setResourceType(
      final ResourceType resourceType) {
    resourceTypeProp.setValue(resourceType);
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

  public DeploymentResourceDefinitionResponseRecord setChecksum(
      final DirectBuffer checksum, final int offset, final int length) {
    checksumProp.setValue(checksum, offset, length);
    return this;
  }

  public DeploymentResourceDefinitionResponseRecord setChecksum(final byte[] checksum) {
    checksumProp.setValue(BufferUtil.wrapArray(checksum));
    return this;
  }

  @Override
  public long getDeploymentId() {
    return deploymentIdProp.getValue();
  }

  public DeploymentResourceDefinitionResponseRecord setDeploymentId(final long deploymentId) {
    deploymentIdProp.setValue(deploymentId);
    return this;
  }

  @Override
  protected DeploymentResourceDefinitionResponseRecord newRecord() {
    return new DeploymentResourceDefinitionResponseRecord();
  }
}
