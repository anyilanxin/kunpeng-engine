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
package com.anyilanxin.kunpeng.repository.business.modules.variable.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.variable.VariableRecord;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.DocumentProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * 变量 Entity：变量 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class VariableRecordEntity extends UnpackedObject implements StoreValue {
  private final LongProperty scopIdProp = new LongProperty(1, "SCOP_ID");
  private final LongProperty parentScopIdProp = new LongProperty(2, "PARENT_SCOPE_ID", -1);
  private final IntegerProperty revProp = new IntegerProperty(3, VERSION, 0);
  private final LongProperty processDefinitionIdProp =
      new LongProperty(4, PROCESS_DEFINITION_ID, -1);
  private final LongProperty processInstanceIdProp = new LongProperty(5, PROCESS_INSTANCE_ID, -1);
  private final DocumentProperty variablesProperty = new DocumentProperty(6, VARIABLES);
  private final StringProperty tenantIdProp =
      new StringProperty(7, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public VariableRecordEntity() {
    super(7);
    declareProperty(scopIdProp)
        .declareProperty(parentScopIdProp)
        .declareProperty(revProp)
        .declareProperty(processDefinitionIdProp)
        .declareProperty(processInstanceIdProp)
        .declareProperty(variablesProperty)
        .declareProperty(tenantIdProp);
  }

  public void wrap(final VariableRecord record) {
    setScopId(record.getScopId())
        .setParentScopId(record.getParentScopId())
        .setRev(record.getRev())
        .setProcessDefinitionId(record.getProcessDefinitionId())
        .setProcessInstanceId(record.getProcessInstanceId())
        .setVariables(record.getVariablesBuffer())
        .setTenantId(record.getTenantIdBuffer());
  }

  public VariableRecord unwrap(final VariableRecord variableRecord) {
    variableRecord.reset();
    return variableRecord
        .setScopId(getScopId())
        .setParentScopId(getParentScopId())
        .setRev(getRev())
        .setProcessDefinitionId(getProcessDefinitionId())
        .setProcessInstanceId(getProcessInstanceId())
        .setVariables(getVariablesBuffer())
        .setTenantId(getTenantIdBuffer());
  }

  public long getScopId() {
    return scopIdProp.getValue();
  }

  public VariableRecordEntity setScopId(final long scopId) {
    scopIdProp.setValue(scopId);
    return this;
  }

  public long getParentScopId() {
    return parentScopIdProp.getValue();
  }

  public VariableRecordEntity setParentScopId(final long parentScopId) {
    parentScopIdProp.setValue(parentScopId);
    return this;
  }

  public int getRev() {
    return revProp.getValue();
  }

  public VariableRecordEntity setRev(final int rev) {
    revProp.setValue(rev);
    return this;
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public VariableRecordEntity setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public VariableRecordEntity setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  public Map<String, Object> getVariables() {
    return convertToMap(variablesProperty.getValue());
  }

  public DirectBuffer getVariablesBuffer() {
    return variablesProperty.getValue();
  }

  public VariableRecordEntity setVariables(final Map<String, Object> variable) {
    variablesProperty.setValue(wrapArray(convertToMsgPack(variable)));
    return this;
  }

  public VariableRecordEntity setVariables(final DirectBuffer variables) {
    variablesProperty.setValue(variables);
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public VariableRecordEntity setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public VariableRecordEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
