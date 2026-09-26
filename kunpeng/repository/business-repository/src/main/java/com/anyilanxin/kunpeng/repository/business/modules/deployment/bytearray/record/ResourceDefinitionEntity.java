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
package com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.DEPLOYMENT_ID;
import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.TENANT_ID;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ResourceDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ResourceType;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 资源定义 Entity：资源定义 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ResourceDefinitionEntity extends UnpackedObject implements StoreValue {
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

  public ResourceDefinitionEntity() {
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

  public void wrap(final ResourceDefinitionRecord record) {
    reset();
    setResourceDefinitionId(record.getResourceDefinitionId())
        .setResourceDefinitionName(record.getResourceDefinitionNameBuffer())
        .setDefinitionVersion(record.getDefinitionVersion())
        .setResourceType(record.getResourceType())
        .setDeploymentId(record.getDeploymentId())
        .setTenantId(record.getTenantIdBuffer());
    final DirectBuffer resourceBuffer = record.getResourceBuffer();
    setResource(resourceBuffer, 0, resourceBuffer.capacity());
    final DirectBuffer checksumBuffer = record.getChecksumBuffer();
    setChecksum(checksumBuffer, 0, checksumBuffer.capacity());
  }

  public ResourceDefinitionRecord unwrap(final ResourceDefinitionRecord record) {
    record.reset();
    final DirectBuffer resourceBuffer = getResourceBuffer();
    final DirectBuffer checksumBuffer = getChecksumBuffer();
    return record
        .setResourceDefinitionId(getResourceDefinitionId())
        .setResourceDefinitionName(getResourceDefinitionNameBuffer())
        .setDefinitionVersion(getDefinitionVersion())
        .setResource(resourceBuffer, 0, resourceBuffer.capacity())
        .setChecksum(checksumBuffer, 0, checksumBuffer.capacity())
        .setDeploymentId(getDeploymentId())
        .setTenantId(getTenantIdBuffer());
  }

  public long getResourceDefinitionId() {
    return resourceDefinitionIdProp.getValue();
  }

  public ResourceDefinitionEntity setResourceDefinitionId(final long resourceDefinitionId) {
    resourceDefinitionIdProp.setValue(resourceDefinitionId);
    return this;
  }

  public String getResourceDefinitionName() {
    return bufferAsString(resourceDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getResourceDefinitionNameBuffer() {
    return resourceDefinitionNameProp.getValue();
  }

  public ResourceDefinitionEntity setResourceDefinitionName(final String resourceDefinitionName) {
    if (resourceDefinitionName != null) {
      resourceDefinitionNameProp.setValue(wrapString(resourceDefinitionName));
    }
    setBytesType();
    return this;
  }

  public ResourceDefinitionEntity setResourceDefinitionName(
      final DirectBuffer resourceDefinitionName) {
    resourceDefinitionNameProp.setValue(resourceDefinitionName);
    setBytesType();
    return this;
  }

  public int getDefinitionVersion() {
    return definitionVersionProp.getValue();
  }

  public ResourceDefinitionEntity setDefinitionVersion(final int definitionVersion) {
    definitionVersionProp.setValue(definitionVersion);
    return this;
  }

  public ResourceType getResourceType() {
    return resourceTypeProp.getValue();
  }

  public ResourceDefinitionEntity setResourceType(final ResourceType resourceType) {
    resourceTypeProp.setValue(resourceType);
    return this;
  }

  public byte[] getResource() {
    return bufferAsArray(resourceProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getResourceBuffer() {
    return resourceProp.getValue();
  }

  public ResourceDefinitionEntity setResource(
      final DirectBuffer resource, final int offset, final int length) {
    resourceProp.setValue(resource, offset, length);
    setChecksum(sha256(getResource()));
    return this;
  }

  public ResourceDefinitionEntity setResource(final byte[] resource) {
    resourceProp.setValue(BufferUtil.wrapArray(resource));
    setChecksum(sha256(resource));
    return this;
  }

  public byte[] getChecksum() {
    return bufferAsArray(checksumProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getChecksumBuffer() {
    return checksumProp.getValue();
  }

  public ResourceDefinitionEntity setChecksum(
      final DirectBuffer checksum, final int offset, final int length) {
    checksumProp.setValue(checksum, offset, length);
    return this;
  }

  public ResourceDefinitionEntity setChecksum(final byte[] checksum) {
    checksumProp.setValue(BufferUtil.wrapArray(checksum));
    return this;
  }

  public long getDeploymentId() {
    return deploymentIdProp.getValue();
  }

  public ResourceDefinitionEntity setDeploymentId(final long deploymentId) {
    deploymentIdProp.setValue(deploymentId);
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public ResourceDefinitionEntity setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public ResourceDefinitionEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  private void setBytesType() {
    final String resourceName = getResourceDefinitionName();
    if (resourceName != null && !resourceName.trim().isEmpty()) {
      final String[] split = resourceName.split("\\.");
      final ResourceType byteArrayType =
          ResourceType.valueOf(split[split.length - 1].toUpperCase());
      resourceTypeProp.setValue(byteArrayType);
    }
  }

  private static byte[] sha256(final byte[] data) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(data);
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
