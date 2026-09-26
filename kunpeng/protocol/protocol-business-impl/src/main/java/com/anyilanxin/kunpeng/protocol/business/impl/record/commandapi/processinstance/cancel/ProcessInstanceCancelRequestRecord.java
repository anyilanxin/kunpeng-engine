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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.cancel;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.PROCESS_INSTANCE_ID;
import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.TENANT_ID;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.cancel.ProcessInstanceCancelRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 流程实例取消请求 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ProcessInstanceCancelRequestRecord
    extends UnifiedRecordValue<ProcessInstanceCancelRequestRecord>
    implements ProcessInstanceCancelRequestRecordValue {
  private final LongProperty processInstanceIdProp = new LongProperty(1, PROCESS_INSTANCE_ID, -1);
  private final StringProperty tenantIdProp =
      new StringProperty(2, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public ProcessInstanceCancelRequestRecord() {
    super(2);
    // formatting:off
    declareProperty(processInstanceIdProp)
        .declareProperty(tenantIdProp);
    // formatting:on
  }

  @Override
  public ProcessInstanceCancelRequestRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  @Override
  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public ProcessInstanceCancelRequestRecord setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public ProcessInstanceCancelRequestRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
