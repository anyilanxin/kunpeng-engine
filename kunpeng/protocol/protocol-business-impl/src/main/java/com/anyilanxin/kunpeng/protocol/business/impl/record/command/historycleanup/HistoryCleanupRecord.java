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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.historycleanup;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 历史数据清理 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class HistoryCleanupRecord extends UnifiedRecordValue<HistoryCleanupRecord>
    implements HistoryCleanupRecordValue {
  // structpack-ids[HistoryCleanupRecord]: 1,2,3,4,5,6,7,8,9,10
  private final LongProperty historyCleanupIdProp = new LongProperty(1, "HISTORY_CLEANUP_ID", -1);
  private final LongProperty processInstanceIdProp = new LongProperty(3, PROCESS_INSTANCE_ID, -1);
  private final StringProperty processDefinitionNameProp =
      new StringProperty(4, PROCESS_DEFINITION_NAME, "");
  private final LongProperty processDefinitionIdProp = new LongProperty(5, PROCESS_DEFINITION_ID);
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(6, PROCESS_DEFINITION_KEY);
  private final LongProperty dueDateProp = new LongProperty(2, "DUE_DATE", 0);
  private final LongProperty startTimeProp = new LongProperty(7, START_TIME, -1);
  private final LongProperty endTimeProp = new LongProperty(8, END_TIME, -1);
  private final LongProperty durationProp = new LongProperty(9, DURATION, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(10, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public HistoryCleanupRecord() {
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

  @Override
  public long getHistoryCleanupId() {
    return historyCleanupIdProp.getValue();
  }

  public HistoryCleanupRecord setHistoryCleanupId(final long historyCleanupId) {
    historyCleanupIdProp.setValue(historyCleanupId);
    return this;
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public HistoryCleanupRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  @Override
  public String getProcessDefinitionName() {
    return bufferAsString(processDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionNameBuffer() {
    return processDefinitionNameProp.getValue();
  }

  public HistoryCleanupRecord setProcessDefinitionName(final String processDefinitionName) {
    if (processDefinitionName != null) {
      processDefinitionNameProp.setValue(wrapString(processDefinitionName));
    }
    return this;
  }

  public HistoryCleanupRecord setProcessDefinitionName(final DirectBuffer processDefinitionName) {
    processDefinitionNameProp.setValue(processDefinitionName);
    return this;
  }

  @Override
  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public HistoryCleanupRecord setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  @Override
  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public HistoryCleanupRecord setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public HistoryCleanupRecord setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  @Override
  public long getDueDate() {
    return dueDateProp.getValue();
  }

  public HistoryCleanupRecord setDueDate(final long dueDate) {
    dueDateProp.setValue(dueDate);
    return this;
  }

  @Override
  public long getStartTime() {
    return startTimeProp.getValue();
  }

  public HistoryCleanupRecord setStartTime(final long startTime) {
    startTimeProp.setValue(startTime);
    return this;
  }

  @Override
  public long getEndTime() {
    return endTimeProp.getValue();
  }

  public HistoryCleanupRecord setEndTime(final long endTime) {
    endTimeProp.setValue(endTime);
    final long startTime = startTimeProp.getValue();
    if (endTime > 0 && startTime > 0) {
      durationProp.setValue(endTime - startTime);
    }
    return this;
  }

  @Override
  public long getDuration() {
    return durationProp.getValue();
  }

  public HistoryCleanupRecord setDuration(final long duration) {
    durationProp.setValue(duration);
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

  public HistoryCleanupRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public HistoryCleanupRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
