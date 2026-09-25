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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import org.agrona.DirectBuffer;

/**
 * 活动处理上下文内容。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ActivityContent {
  private final ActivityInstanceRecord value;
  private final long requestId;
  private final long operationReferenceKey;
  private final long batchOperationReference;

  public ActivityContent(
      final ActivityInstanceRecord value,
      final long requestId,
      final long operationReferenceKey,
      final long batchOperationReference) {
    this.value = value;
    this.requestId = requestId;
    this.operationReferenceKey = operationReferenceKey;
    this.batchOperationReference = batchOperationReference;
  }

  public ActivityContent(final ActivityInstanceRecord value, final long requestId) {
    this(value, requestId, -1, -1);
  }

  public ActivityInstanceRecord getValue() {
    return value;
  }

  public long getOperationReferenceKey() {
    return operationReferenceKey;
  }

  public long getBatchOperationReference() {
    return batchOperationReference;
  }

  public BpmnElementType getElementType() {
    return value.getActivityDefinitionType();
  }

  public long getRequestId() {
    return requestId;
  }

  public String getActivityDefinitionKey() {
    return value.getActivityDefinitionKey();
  }

  public DirectBuffer getActivityDefinitionKeyBuffer() {
    return value.getActivityDefinitionKeyBuffer();
  }

  public DirectBuffer getTenantIdBuffer() {
    return value.getTenantIdBuffer();
  }

  public String getTenantId() {
    return value.getTenantId();
  }

  public String getActivityDefinitionName() {
    return value.getActivityDefinitionName();
  }

  public DirectBuffer getActivityDefinitionNameBuffer() {
    return value.getActivityDefinitionNameBuffer();
  }

  public String getProcessDefinitionKey() {
    return value.getProcessDefinitionKey();
  }

  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return value.getProcessDefinitionKeyBuffer();
  }

  public long getProcessDefinitionId() {
    return value.getProcessDefinitionId();
  }

  public long getProcessInstanceId() {
    return value.getProcessInstanceId();
  }

  public long getRootProcessInstanceId() {
    return value.getRootProcessInstanceId();
  }

  public long getParentActivityInstanceId() {
    return value.getParentActivityInstanceId();
  }

  public long getFeedBackParentActivityInstanceId() {
    return value.getFeedBackParentActivityInstanceId();
  }

  public boolean isFeedBackParentActivityInstance() {
    return value.getFeedBackParentActivityInstanceId() != -1;
  }

  public long getActivityInstanceId() {
    return value.getActivityInstanceId();
  }

  public ActivityInstanceRecord copyBase() {
    return value.copyBase();
  }

  public ActivityInstanceRecord copy() {
    return value.copy();
  }

  public ActivityContent updateValue(final ActivityInstanceRecord value) {
    return new ActivityContent(value, requestId);
  }
}
