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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.create;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.create.ProcessInstanceCreateRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 流程实例创建请求 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ProcessInstanceCreateRequestRecord
    extends UnifiedRecordValue<ProcessInstanceCreateRequestRecord>
    implements ProcessInstanceCreateRequestRecordValue {
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(2, PROCESS_DEFINITION_KEY, "");
  private final IntegerProperty processDefinitionVersionProp =
      new IntegerProperty(3, PROCESS_DEFINITION_VERSION, -1);
  private final LongProperty processDefinitionIdProp =
      new LongProperty(4, PROCESS_DEFINITION_ID, -1);
  private final DocumentProperty variablesProperty = new DocumentProperty(5, VARIABLES);
  private final ArrayProperty<StringValue> activityDefinitionKeysProp =
      new ArrayProperty<>(1, "ACTIVITY_DEFINITION_KEYS", StringValue::new);
  private final StringProperty tenantIdProp =
      new StringProperty(6, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);
  private final StringProperty businessKeyProp = new StringProperty(7, BUSINESS_KEY, "");

  public ProcessInstanceCreateRequestRecord() {
    super(7);
    declareProperty(processDefinitionKeyProp)
        .declareProperty(processDefinitionVersionProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(variablesProperty)
        .declareProperty(activityDefinitionKeysProp)
        .declareProperty(tenantIdProp)
        .declareProperty(businessKeyProp);
  }

  @Override
  public ProcessInstanceCreateRequestRecord setProcessDefinitionKey(
      final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  @Override
  public ProcessInstanceCreateRequestRecord setProcessDefinitionKey(
      final String processDefinitionKey, final int ProcessDefinitionVersion) {
    setProcessDefinitionKey(processDefinitionKey);
    processDefinitionVersionProp.setValue(ProcessDefinitionVersion);
    return this;
  }

  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  @Override
  public ProcessInstanceCreateRequestRecord setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public int getProcessDefinitionVersion() {
    return processDefinitionVersionProp.getValue();
  }

  @Override
  public ProcessInstanceCreateRequestRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  @Override
  public ProcessInstanceCreateRequestRecord addActivityDefinitionKey(
      final String activityDefinitionKey) {
    activityDefinitionKeysProp.add().wrap(wrapString(activityDefinitionKey));
    return this;
  }

  public ArrayProperty<StringValue> activityDefinitionKeys() {
    return activityDefinitionKeysProp;
  }

  @Override
  public ProcessInstanceCreateRequestRecord setVariables(final Map<String, Object> variables) {
    variablesProperty.setValue(wrapArray(convertToMsgPack(variables)));
    return this;
  }

  @Override
  public ProcessInstanceCreateRequestRecordValue setVariables(final DirectBuffer variables) {
    variablesProperty.setValue(variables);
    return this;
  }

  public Map<String, Object> getVariables() {
    return convertToMap(variablesProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return variablesProperty.getValue();
  }

  @Override
  public ProcessInstanceCreateRequestRecord setBusinessKey(final String businessKey) {
    businessKeyProp.setValue(wrapString(businessKey));
    return this;
  }

  public String getBusinessKey() {
    return bufferAsString(businessKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getBusinessKeyBuffer() {
    return businessKeyProp.getValue();
  }
}
