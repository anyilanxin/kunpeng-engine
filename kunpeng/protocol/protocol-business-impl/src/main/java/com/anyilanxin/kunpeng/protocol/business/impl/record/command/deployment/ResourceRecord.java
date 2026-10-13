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

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ResourceRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ResourceType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.BinaryProperty;
import com.anyilanxin.kunpeng.structpack.property.DocumentProperty;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 资源 Record：部署资源的内容与元数据。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ResourceRecord extends UnifiedRecordValue<ResourceRecord>
    implements ResourceRecordValue {
  private final StringProperty resourceNameProp = new StringProperty(1, "RESOURCE_NAME", "");
  private final EnumProperty<ResourceType> resourceTypeProp =
      new EnumProperty<>(2, "RESOURCE_TYPE", ResourceType.class, ResourceType.NULL_VAL);
  private final BinaryProperty bytesProp = new BinaryProperty(3, "BYTES", new UnsafeBuffer());

  private final DocumentProperty extendedDataProp = new DocumentProperty(4, "EXTENDED_DATA");

  public ResourceRecord() {
    super(4);
    // formatting:off
    declareProperty(resourceNameProp)
        .declareProperty(resourceTypeProp)
        .declareProperty(bytesProp)
        .declareProperty(extendedDataProp);
    // formatting:on
  }

  @Override
  public byte[] getBytes() {
    return bufferAsArray(bytesProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getBytesBuffer() {
    return bytesProp.getValue();
  }

  public ResourceRecord setBytes(final DirectBuffer byteArray, final int offset, final int length) {
    bytesProp.setValue(byteArray, offset, length);
    return this;
  }

  public ResourceRecord setBytes(final byte[] bytes) {
    bytesProp.setValue(BufferUtil.wrapArray(bytes));
    return this;
  }

  @Override
  public ResourceType getResourceType() {
    return resourceTypeProp.getValue();
  }

  @Override
  public String getResourceName() {
    return bufferAsString(resourceNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getResourceNameBuffer() {
    return resourceNameProp.getValue();
  }

  public ResourceRecord setResourceName(final String resourceName) {
    resourceNameProp.setValue(wrapString(resourceName));
    setBytesType();
    return this;
  }

  public ResourceRecord setResourceName(final DirectBuffer resourceName) {
    resourceNameProp.setValue(resourceName);
    setBytesType();
    return this;
  }

  private void setBytesType() {
    final String resourceName = getResourceName();
    final String[] split = resourceName.split("\\.");
    final ResourceType byteArrayType = ResourceType.valueOf(split[split.length - 1].toUpperCase());
    resourceTypeProp.setValue(byteArrayType);
  }

  @Override
  public Map<String, Object> getExtendedData() {
    return convertToMap(extendedDataProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getExtendedDataBuffer() {
    return extendedDataProp.getValue();
  }

  public ResourceRecord setExtendedData(final Map<String, Object> extendedData) {
    extendedDataProp.setValue(wrapArray(convertToMsgPack(extendedData)));
    return this;
  }

  public ResourceRecord setExtendedData(final DirectBuffer extendedData) {
    extendedDataProp.setValue(extendedData);
    return this;
  }
}
