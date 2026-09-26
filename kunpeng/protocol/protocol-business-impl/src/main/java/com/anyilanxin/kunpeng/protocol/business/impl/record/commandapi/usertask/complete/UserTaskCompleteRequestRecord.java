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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.complete;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.complete.UserTaskCompleteRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.DocumentProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 用户任务完成请求 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class UserTaskCompleteRequestRecord extends UnifiedRecordValue<UserTaskCompleteRequestRecord>
    implements UserTaskCompleteRequestRecordValue {
  private final LongProperty taskIdProp = new LongProperty(2, TASK_ID);
  private final DocumentProperty variablesProperty = new DocumentProperty(3, VARIABLES);
  private final DocumentProperty localVariablesProperty =
      new DocumentProperty(1, "LOCAL_VARIABLES");
  private final StringProperty tenantIdProp =
      new StringProperty(4, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public UserTaskCompleteRequestRecord() {
    super(4);
    declareProperty(taskIdProp)
        .declareProperty(variablesProperty)
        .declareProperty(localVariablesProperty)
        .declareProperty(tenantIdProp);
  }

  public long getTaskId() {
    return taskIdProp.getValue();
  }

  public UserTaskCompleteRequestRecord setTaskId(final long taskId) {
    taskIdProp.setValue(taskId);
    return this;
  }

  public Map<String, Object> getVariables() {
    return convertToMap(variablesProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return variablesProperty.getValue();
  }

  public UserTaskCompleteRequestRecord setVariables(final DirectBuffer variables) {
    variablesProperty.setValue(variables);
    return this;
  }

  public UserTaskCompleteRequestRecord setVariables(final Map<String, Object> variables) {
    variablesProperty.setValue(wrapArray(convertToMsgPack(variables)));
    return this;
  }

  public Map<String, Object> getLocalVariables() {
    return convertToMap(localVariablesProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getLocalVariablesBuffer() {
    return localVariablesProperty.getValue();
  }

  public UserTaskCompleteRequestRecord setLocalVariables(final DirectBuffer localVariables) {
    localVariablesProperty.setValue(localVariables);
    return this;
  }

  public UserTaskCompleteRequestRecord setLocalVariables(final Map<String, Object> localVariables) {
    localVariablesProperty.setValue(wrapArray(convertToMsgPack(localVariables)));
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

  public UserTaskCompleteRequestRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  public UserTaskCompleteRequestRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  @Override
  protected UserTaskCompleteRequestRecord newRecord() {
    return new UserTaskCompleteRequestRecord();
  }
}
