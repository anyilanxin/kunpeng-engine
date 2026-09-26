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
package com.anyilanxin.kunpeng.repository.business.modules.historycleanup.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.historycleanup.HistoryCleanupRecord;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 历史清理 Entity：历史清理 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class HistoryCleanupEntity extends UnpackedObject implements StoreValue {
  private final LongProperty historyCleanupIdProp = new LongProperty(1, "HISTORY_CLEANUP_ID", -1);
  private final LongProperty processInstanceIdProp = new LongProperty(2, PROCESS_INSTANCE_ID, -1);
  private final StringProperty processDefinitionNameProp =
      new StringProperty(3, PROCESS_DEFINITION_NAME, "");
  private final LongProperty processDefinitionIdProp = new LongProperty(4, PROCESS_DEFINITION_ID);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(5, PROCESS_DEFINITION_KEY);
  private final LongProperty dueDateProp = new LongProperty(6, "DUE_DATE", 0);
  private final LongProperty startTimeProp = new LongProperty(7, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(8, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(9, DURATION, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(10, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public HistoryCleanupEntity() {
    super(10);
    declareProperty(historyCleanupIdProp)
        .declareProperty(processInstanceIdProp)
        .declareProperty(processDefinitionNameProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(processDefinitionKeyProp)
        .declareProperty(dueDateProp)
        .declareProperty(startTimeProp)
        .declareProperty(endTimeProp)
        .declareProperty(durationProp)
        .declareProperty(tenantIdProp);
  }

  public void wrap(final HistoryCleanupRecord record) {
    setHistoryCleanupId(record.getHistoryCleanupId())
        .setProcessInstanceId(record.getProcessInstanceId())
        .setProcessDefinitionName(record.getProcessDefinitionNameBuffer())
        .setProcessDefinitionId(record.getProcessDefinitionId())
        .setDueDate(record.getDueDate())
        .setStartTime(record.getStartTime())
        .setEndTime(record.getEndTime())
        .setDueDate(record.getDueDate())
        .setTenantId(record.getTenantIdBuffer());
  }

  public HistoryCleanupRecord unwrap(final HistoryCleanupRecord historyCleanupRecord) {
    historyCleanupRecord.reset();
    return historyCleanupRecord
        .setHistoryCleanupId(getHistoryCleanupId())
        .setProcessInstanceId(getProcessInstanceId())
        .setProcessDefinitionName(getProcessDefinitionNameBuffer())
        .setProcessDefinitionId(getProcessDefinitionId())
        .setDueDate(getDueDate())
        .setStartTime(getStartTime())
        .setEndTime(getEndTime())
        .setDueDate(getDueDate())
        .setTenantId(getTenantIdBuffer());
  }

  public long getHistoryCleanupId() {
    return historyCleanupIdProp.getValue();
  }

  public HistoryCleanupEntity setHistoryCleanupId(final long historyCleanupId) {
    historyCleanupIdProp.setValue(historyCleanupId);
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public HistoryCleanupEntity setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  public String getProcessDefinitionName() {
    return bufferAsString(processDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionNameBuffer() {
    return processDefinitionNameProp.getValue();
  }

  public HistoryCleanupEntity setProcessDefinitionName(final String processDefinitionName) {
    if (processDefinitionName != null) {
      processDefinitionNameProp.setValue(wrapString(processDefinitionName));
    }
    return this;
  }

  public HistoryCleanupEntity setProcessDefinitionName(final DirectBuffer processDefinitionName) {
    processDefinitionNameProp.setValue(processDefinitionName);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public HistoryCleanupEntity setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public HistoryCleanupEntity setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public HistoryCleanupEntity setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public long getDueDate() {
    return dueDateProp.getValue();
  }

  public HistoryCleanupEntity setDueDate(final long dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public HistoryCleanupEntity setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public HistoryCleanupEntity setEndTime(final long endTime) {
    endTimeProp.setValue(endTime);
    final long startTime = startTimeProp.getValue();
    if (endTime > 0 && startTime > 0) {
      durationProp.setValue(endTime - startTime);
    }
    return this;
  }

  public long getDuration() {
    return durationProp.getValue();
  }

  public HistoryCleanupEntity setDuration(final long duration) {
    durationProp.setValue(duration);
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public HistoryCleanupEntity setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public HistoryCleanupEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
