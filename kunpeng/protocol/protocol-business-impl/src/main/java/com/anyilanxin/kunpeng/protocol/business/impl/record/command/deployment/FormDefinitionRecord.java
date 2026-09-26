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

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.TENANT_ID;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsArray;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;

import com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.FormDefinitionRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.BinaryProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 表单定义 Record：部署的表单资源元数据。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class FormDefinitionRecord extends UnifiedRecordValue<FormDefinitionRecord>
    implements FormDefinitionRecordValue {
  private final LongProperty decisionDefinitionIdProp =
      new LongProperty(1, "DECISION_DEFINITION_ID");
  private final IntegerProperty revProp = new IntegerProperty(6, BusinessRecordConstant.VERSION, 0);
  private final LongProperty deploymentIdProp = new LongProperty(2, "DEPLOYMENT_ID");
  private final LongProperty byteArrayIdProp = new LongProperty(3, "BYTE_ARRAY_ID");
  private final StringProperty byteArrayNameProp = new StringProperty(4, "BYTE_ARRAY_NAME");
  private final BinaryProperty byteArrayProp =
      new BinaryProperty(5, "BYTE_ARRAY", new UnsafeBuffer());
  private final StringProperty tenantIdProp =
      new StringProperty(7, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public FormDefinitionRecord() {
    super(7);
    declareProperty(decisionDefinitionIdProp)
        .declareProperty(revProp)
        .declareProperty(deploymentIdProp)
        .declareProperty(byteArrayIdProp)
        .declareProperty(byteArrayNameProp)
        .declareProperty(byteArrayProp)
        .declareProperty(tenantIdProp);
  }

  @Override
  public long getFormDefinitionId() {
    return decisionDefinitionIdProp.getValue();
  }

  @Override
  public int getRev() {
    return revProp.getValue();
  }

  @Override
  public long getDeploymentId() {
    return deploymentIdProp.getValue();
  }

  @Override
  public long getByteArrayId() {
    return byteArrayIdProp.getValue();
  }

  @Override
  public String getByteArrayName() {
    return bufferAsString(byteArrayNameProp.getValue());
  }

  @Override
  public byte[] getByteArray() {
    return bufferAsArray(byteArrayProp.getValue());
  }

  @Override
  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @Override
  protected FormDefinitionRecord newRecord() {
    return new FormDefinitionRecord();
  }
}
