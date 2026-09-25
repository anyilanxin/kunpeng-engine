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
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;

import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ResourceDefinitionValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ResourceType;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 资源定义记录(独立于流程定义/决策定义,描述部署资源元数据)
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ResourceDefinitionRecord extends UnifiedRecordValue<ResourceDefinitionRecord>
    implements ResourceDefinitionValue {
  // structpack-ids[ResourceDefinitionRecord]: 1,2,3,4,5,6,7,8
  private final LongProperty resourceDefinitionIdProp =
      new LongProperty(1, "RESOURCE_DEFINITION_ID", -1);
  private final StringProperty resourceDefinitionNameProp =
      new StringProperty(2, "RESOURCE_DEFINITION_NAME", "");
  private final IntegerProperty definitionVersionProp =
      new IntegerProperty(3, "DEFINITION_VERSION", -1);
  private final EnumProperty<ResourceType> resourceTypeProp =
      new EnumProperty<>(4, "RESOURCE_TYPE", ResourceType.class, ResourceType.NULL_VAL);
  private final BinaryProperty resourceProp = new BinaryProperty(5, "RESOURCE", new UnsafeBuffer());
  private final BinaryProperty checksumProp = new BinaryProperty(6, "CHECKSUM", new UnsafeBuffer());
  private final LongProperty deploymentIdProp = new LongProperty(7, DEPLOYMENT_ID, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(8, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public ResourceDefinitionRecord() {
    super(8);
    declareProperty(resourceDefinitionIdProp)
        .declareProperty(resourceDefinitionNameProp)
        .declareProperty(definitionVersionProp)
        .declareProperty(resourceTypeProp)
        .declareProperty(resourceProp)
        .declareProperty(checksumProp)
        .declareProperty(deploymentIdProp)
        .declareProperty(tenantIdProp);
  }

  @Override
  public long getResourceDefinitionId() {
    return resourceDefinitionIdProp.getValue();
  }

  public ResourceDefinitionRecord setResourceDefinitionId(final long resourceId) {
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

  public ResourceDefinitionRecord setResourceDefinitionName(final String resourceName) {
    if (resourceName != null) {
      resourceDefinitionNameProp.setValue(wrapString(resourceName));
    }
    setBytesType();
    return this;
  }

  public ResourceDefinitionRecord setResourceDefinitionName(final DirectBuffer resourceName) {
    resourceDefinitionNameProp.setValue(resourceName);
    setBytesType();
    return this;
  }

  @Override
  public int getDefinitionVersion() {
    return definitionVersionProp.getValue();
  }

  public ResourceDefinitionRecord setDefinitionVersion(final int definitionVersion) {
    definitionVersionProp.setValue(definitionVersion);
    return this;
  }

  @Override
  public ResourceType getResourceType() {
    return resourceTypeProp.getValue();
  }

  @Override
  public byte[] getResource() {
    return bufferAsArray(resourceProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getResourceBuffer() {
    return resourceProp.getValue();
  }

  public ResourceDefinitionRecord setResource(
      final DirectBuffer resource, final int offset, final int length) {
    resourceProp.setValue(resource, offset, length);
    setChecksum(DigestUtils.sha256(getResource()));
    return this;
  }

  public ResourceDefinitionRecord setResource(final byte[] resource) {
    resourceProp.setValue(BufferUtil.wrapArray(resource));
    setChecksum(DigestUtils.sha256(resource));
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

  private ResourceDefinitionRecord setChecksum(final byte[] checksum) {
    checksumProp.setValue(BufferUtil.wrapArray(checksum));
    return this;
  }

  public ResourceDefinitionRecord setChecksum(
      final DirectBuffer checksum, final int offset, final int length) {
    checksumProp.setValue(checksum, offset, length);
    return this;
  }

  @Override
  public long getDeploymentId() {
    return deploymentIdProp.getValue();
  }

  public ResourceDefinitionRecord setDeploymentId(final long deploymentId) {
    deploymentIdProp.setValue(deploymentId);
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

  public ResourceDefinitionRecord setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public ResourceDefinitionRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  @Override
  protected ResourceDefinitionRecord newRecord() {
    return new ResourceDefinitionRecord();
  }

  private void setBytesType() {
    final String resourceName = getResourceDefinitionName();
    if (StringUtils.isNotBlank(resourceName)) {
      final String[] split = resourceName.split("\\.");
      final ResourceType byteArrayType =
          ResourceType.valueOf(split[split.length - 1].toUpperCase());
      resourceTypeProp.setValue(byteArrayType);
    }
  }
}
