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
package com.anyilanxin.kunpeng.repository.business.modules.batch.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.TENANT_ID;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.BatchBusinessType;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.ShortProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import org.agrona.DirectBuffer;

/**
 * 批量操作 Entity：流程实例批量 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class BatchEntity extends UnpackedObject implements StoreValue {
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

  public BatchEntity() {
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

  public void wrap(final ProcessInstanceBatchRecord record) {
    setBatchId(record.getBatchId())
        .setRequestId(record.getRequestId())
        .setBatchValueType(record.getBatchValueType())
        .setBatchLifeCycle(record.getBatchLifeCycle())
        .setBatchBusinessId(record.getBatchBusinessId())
        .setBatchType(record.getBatchType())
        .setCurrentExecutionId(record.getCurrentExecutionId())
        .setBatchAfterValueType(record.getBatchAfterValueType())
        .setBatchAfterLifeCycle(record.getBatchAfterLifeCycle())
        .setBatchAfterBusinessId(record.getBatchAfterBusinessId())
        .setBatchAfterType(record.getBatchAfterType())
        .setBatchAfterBatchOperationReference(record.getBatchAfterBatchOperationReference())
        .setBatchAfterOperationReference(record.getBatchAfterOperationReference())
        .setTenantId(record.getTenantIdBuffer());
  }

  public ProcessInstanceBatchRecord unwrap(final ProcessInstanceBatchRecord batchRecord) {
    return batchRecord
        .setBatchId(getBatchId())
        .setRequestId(getRequestId())
        .setBatchValueType(getBatchValueType())
        .setBatchLifeCycle(getBatchLifeCycle())
        .setBatchBusinessId(getBatchBusinessId())
        .setBatchType(getBatchType())
        .setCurrentExecutionId(getCurrentExecutionId())
        .setBatchAfterValueType(getBatchAfterValueType())
        .setBatchAfterLifeCycle(getBatchAfterLifeCycle())
        .setBatchAfterBusinessId(getBatchAfterBusinessId())
        .setBatchAfterType(getBatchAfterType())
        .setBatchAfterBatchOperationReference(getBatchAfterBatchOperationReference())
        .setBatchAfterOperationReference(getBatchAfterOperationReference())
        .setTenantId(getTenantIdBuffer());
  }

  public long getBatchId() {
    return batchIdProp.getValue();
  }

  public BatchEntity setBatchId(final long batchId) {
    batchIdProp.setValue(batchId);
    return this;
  }

  public long getRequestId() {
    return requestIdProp.getValue();
  }

  public BatchEntity setRequestId(final long requestId) {
    requestIdProp.setValue(requestId);
    return this;
  }

  public ValueType getBatchValueType() {
    return batchValueTypeProp.getValue();
  }

  public BatchEntity setBatchValueType(final ValueType batchValueType) {
    batchValueTypeProp.setValue(batchValueType);
    return this;
  }

  public ValueLifeCycle getBatchLifeCycle() {
    return ValueLifeCycle.fromProtocolValue(getBatchValueType(), batchLifeCycleProp.getValue());
  }

  public BatchEntity setBatchLifeCycle(final ValueLifeCycle batchAfterLifeCycle) {
    batchLifeCycleProp.setValue(batchAfterLifeCycle.value());
    return this;
  }

  public long getBatchBusinessId() {
    return batchBusinessIdProp.getValue();
  }

  public BatchEntity setBatchBusinessId(final long batchBusinessId) {
    batchBusinessIdProp.setValue(batchBusinessId);
    return this;
  }

  public BatchBusinessType getBatchType() {
    return batchTypeProp.getValue();
  }

  public BatchEntity setBatchType(final BatchBusinessType batchAfterType) {
    batchTypeProp.setValue(batchAfterType);
    return this;
  }

  public long getCurrentExecutionId() {
    return currentExecutionIdProp.getValue();
  }

  public BatchEntity setCurrentExecutionId(final long currentExecutionId) {
    currentExecutionIdProp.setValue(currentExecutionId);
    return this;
  }

  public ValueType getBatchAfterValueType() {
    return batchAfterValueTypeProp.getValue();
  }

  public BatchEntity setBatchAfterValueType(final ValueType batchAfterValueType) {
    batchAfterValueTypeProp.setValue(batchAfterValueType);
    return this;
  }

  public ValueLifeCycle getBatchAfterLifeCycle() {
    return ValueLifeCycle.fromProtocolValue(
        getBatchAfterValueType(), batchAfterLifeCycleProp.getValue());
  }

  public BatchEntity setBatchAfterLifeCycle(final ValueLifeCycle batchAfterLifeCycle) {
    batchAfterLifeCycleProp.setValue(batchAfterLifeCycle.value());
    return this;
  }

  public long getBatchAfterBusinessId() {
    return batchAfterBusinessIdProp.getValue();
  }

  public BatchEntity setBatchAfterBusinessId(final long batchAfterBusinessId) {
    batchAfterBusinessIdProp.setValue(batchAfterBusinessId);
    return this;
  }

  public BatchBusinessType getBatchAfterType() {
    return batchAfterTypeProp.getValue();
  }

  public BatchEntity setBatchAfterType(final BatchBusinessType batchAfterType) {
    batchAfterTypeProp.setValue(batchAfterType);
    return this;
  }

  public long getBatchAfterOperationReference() {
    return batchAfterOperationReferenceProp.getValue();
  }

  public BatchEntity setBatchAfterOperationReference(final long batchAfterOperationReference) {
    batchAfterOperationReferenceProp.setValue(batchAfterOperationReference);
    return this;
  }

  public long getBatchAfterBatchOperationReference() {
    return batchAfterBatchOperationReferenceProp.getValue();
  }

  public BatchEntity setBatchAfterBatchOperationReference(
      final long batchAfterBatchOperationReference) {
    batchAfterBatchOperationReferenceProp.setValue(batchAfterBatchOperationReference);
    return this;
  }

  public String getTenantId() {
    return BufferUtil.bufferAsString(tenantIdProp.getValue());
  }

  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public BatchEntity setTenantId(final String tenantId) {
    tenantIdProp.setValue(BufferUtil.wrapString(tenantId));
    return this;
  }

  public BatchEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
