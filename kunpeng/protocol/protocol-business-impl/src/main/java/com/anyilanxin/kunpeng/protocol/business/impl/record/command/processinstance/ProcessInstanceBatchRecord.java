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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.TENANT_ID;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.BatchBusinessType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.ShortProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import org.agrona.DirectBuffer;

/**
 * 流程实例批量操作 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ProcessInstanceBatchRecord extends UnifiedRecordValue<ProcessInstanceBatchRecord>
    implements ProcessInstanceBatchRecordValue {
  private final LongProperty batchIdProp = new LongProperty(1, "BATCH_ID");
  private final LongProperty requestIdProp = new LongProperty(2, "REQUEST_ID");
  private final EnumProperty<ValueType> batchValueTypeProp =
      new EnumProperty<>(3, "BATCH_VALUE_TYPE", ValueType.class);
  private final ShortProperty batchLifeCycleProp = new ShortProperty(4, "BATCH_LIFE_CYCLE");
  private final LongProperty batchBusinessIdProp = new LongProperty(5, "BATCH_BUSINESS_ID");
  private final EnumProperty<BatchBusinessType> batchTypeProp =
      new EnumProperty<>(6, "BATCH_TYPE", BatchBusinessType.class);
  private final LongProperty currentExecutionIdProp =
      new LongProperty(7, "CURRENT_EXECUTION_ID", -1);
  private final EnumProperty<ValueType> batchAfterValueTypeProp =
      new EnumProperty<>(8, "BATCH_AFTER_VALUE_TYPE", ValueType.class);
  private final ShortProperty batchAfterLifeCycleProp =
      new ShortProperty(9, "BATCH_AFTER_LIFE_CYCLE");
  private final LongProperty batchAfterBusinessIdProp =
      new LongProperty(10, "BATCH_AFTER_BUSINESS_ID");
  private final EnumProperty<BatchBusinessType> batchAfterTypeProp =
      new EnumProperty<>(11, "BATCH_AFTER_TYPE", BatchBusinessType.class);
  private final LongProperty batchAfterOperationReferenceProp =
      new LongProperty(12, "BATCH_AFTER_OPERATION_REFERENCE", -1);
  private final LongProperty batchAfterBatchOperationReferenceProp =
      new LongProperty(13, "BATCH_AFTER_BATCH_OPERATION_REFERENCE", -1);
  private final StringProperty tenantIdProp =
      new StringProperty(14, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public ProcessInstanceBatchRecord() {
    super(14);
    declareProperty(batchIdProp)
        .declareProperty(requestIdProp)
        .declareProperty(batchValueTypeProp)
        .declareProperty(batchLifeCycleProp)
        .declareProperty(batchBusinessIdProp)
        .declareProperty(batchTypeProp)
        .declareProperty(currentExecutionIdProp)
        .declareProperty(batchAfterValueTypeProp)
        .declareProperty(batchAfterLifeCycleProp)
        .declareProperty(batchAfterBusinessIdProp)
        .declareProperty(batchAfterTypeProp)
        .declareProperty(batchAfterOperationReferenceProp)
        .declareProperty(batchAfterBatchOperationReferenceProp)
        .declareProperty(tenantIdProp);
  }

  @Override
  public long getBatchId() {
    return batchIdProp.getValue();
  }

  public ProcessInstanceBatchRecord setBatchId(final long batchId) {
    batchIdProp.setValue(batchId);
    return this;
  }

  @Override
  public long getRequestId() {
    return requestIdProp.getValue();
  }

  public ProcessInstanceBatchRecord setRequestId(final long requestId) {
    requestIdProp.setValue(requestId);
    return this;
  }

  @Override
  public ValueType getBatchValueType() {
    return batchValueTypeProp.getValue();
  }

  public ProcessInstanceBatchRecord setBatchValueType(final ValueType batchValueType) {
    batchValueTypeProp.setValue(batchValueType);
    return this;
  }

  @Override
  public ValueLifeCycle getBatchLifeCycle() {
    return ValueLifeCycle.fromProtocolValue(getBatchValueType(), batchLifeCycleProp.getValue());
  }

  public ProcessInstanceBatchRecord setBatchLifeCycle(final ValueLifeCycle batchAfterLifeCycle) {
    batchLifeCycleProp.setValue(batchAfterLifeCycle.value());
    return this;
  }

  @Override
  public long getBatchBusinessId() {
    return batchBusinessIdProp.getValue();
  }

  public ProcessInstanceBatchRecord setBatchBusinessId(final long batchBusinessId) {
    batchBusinessIdProp.setValue(batchBusinessId);
    return this;
  }

  @Override
  public BatchBusinessType getBatchType() {
    return batchTypeProp.getValue();
  }

  public ProcessInstanceBatchRecord setBatchType(final BatchBusinessType batchAfterType) {
    batchTypeProp.setValue(batchAfterType);
    return this;
  }

  @Override
  public long getCurrentExecutionId() {
    return currentExecutionIdProp.getValue();
  }

  public ProcessInstanceBatchRecord setCurrentExecutionId(final long currentExecutionId) {
    currentExecutionIdProp.setValue(currentExecutionId);
    return this;
  }

  @Override
  public ValueType getBatchAfterValueType() {
    return batchAfterValueTypeProp.getValue();
  }

  public ProcessInstanceBatchRecord setBatchAfterValueType(final ValueType batchAfterValueType) {
    batchAfterValueTypeProp.setValue(batchAfterValueType);
    return this;
  }

  @Override
  public ValueLifeCycle getBatchAfterLifeCycle() {
    return ValueLifeCycle.fromProtocolValue(
        getBatchAfterValueType(), batchAfterLifeCycleProp.getValue());
  }

  public ProcessInstanceBatchRecord setBatchAfterLifeCycle(
      final ValueLifeCycle batchAfterLifeCycle) {
    batchAfterLifeCycleProp.setValue(batchAfterLifeCycle.value());
    return this;
  }

  @Override
  public long getBatchAfterBusinessId() {
    return batchAfterBusinessIdProp.getValue();
  }

  public ProcessInstanceBatchRecord setBatchAfterBusinessId(final long batchAfterBusinessId) {
    batchAfterBusinessIdProp.setValue(batchAfterBusinessId);
    return this;
  }

  @Override
  public BatchBusinessType getBatchAfterType() {
    return batchAfterTypeProp.getValue();
  }

  public ProcessInstanceBatchRecord setBatchAfterType(final BatchBusinessType batchAfterType) {
    batchAfterTypeProp.setValue(batchAfterType);
    return this;
  }

  @Override
  public long getBatchAfterOperationReference() {
    return batchAfterOperationReferenceProp.getValue();
  }

  public ProcessInstanceBatchRecord setBatchAfterOperationReference(
      final long batchAfterOperationReference) {
    batchAfterOperationReferenceProp.setValue(batchAfterOperationReference);
    return this;
  }

  @Override
  public long getBatchAfterBatchOperationReference() {
    return batchAfterBatchOperationReferenceProp.getValue();
  }

  public ProcessInstanceBatchRecord setBatchAfterBatchOperationReference(
      final long batchAfterBatchOperationReference) {
    batchAfterBatchOperationReferenceProp.setValue(batchAfterBatchOperationReference);
    return this;
  }

  @Override
  public String getTenantId() {
    return BufferUtil.bufferAsString(tenantIdProp.getValue());
  }

  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public ProcessInstanceBatchRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(BufferUtil.wrapString(tenantId));
    return this;
  }

  public ProcessInstanceBatchRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  @Override
  protected ProcessInstanceBatchRecord newRecord() {
    return new ProcessInstanceBatchRecord();
  }
}
