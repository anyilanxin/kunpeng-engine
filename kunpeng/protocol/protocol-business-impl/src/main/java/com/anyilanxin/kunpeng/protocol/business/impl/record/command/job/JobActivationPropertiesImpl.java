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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.job;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;

import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import java.util.Collection;
import org.agrona.DirectBuffer;

/**
 * job 激活属性实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobActivationPropertiesImpl extends UnpackedObject implements JobActivationProperties {
  private final StringProperty workerProp = new StringProperty(1, "worker", "");
  private final LongProperty timeoutProp = new LongProperty(2, "timeout", -1);
  private final ArrayProperty<StringValue> fetchVariablesProp =
      new ArrayProperty<>(3, "variables", StringValue::new);
  private final ArrayProperty<StringValue> tenantIdsProp =
      new ArrayProperty<>(
          4, "tenantIds", () -> new StringValue(TenantOwned.DEFAULT_TENANT_IDENTIFIER));

  public JobActivationPropertiesImpl() {
    super(4);
    declareProperty(workerProp)
        .declareProperty(timeoutProp)
        .declareProperty(fetchVariablesProp)
        .declareProperty(tenantIdsProp);
  }

  public JobActivationPropertiesImpl setWorker(
      final DirectBuffer worker, final int offset, final int length) {
    workerProp.setValue(worker, offset, length);
    return this;
  }

  public JobActivationPropertiesImpl setTimeout(final long val) {
    timeoutProp.setValue(val);
    return this;
  }

  public JobActivationPropertiesImpl setFetchVariables(final Collection<StringValue> variables) {
    fetchVariablesProp.reset();
    variables.forEach(variable -> fetchVariablesProp.add().wrap(variable.getValue()));
    return this;
  }

  public JobActivationPropertiesImpl setTenantIds(final Collection<String> tenantIds) {
    tenantIdsProp.reset();
    tenantIds.forEach(tenantId -> tenantIdsProp.add().wrap(BufferUtil.wrapString(tenantId)));
    return this;
  }

  @Override
  public DirectBuffer worker() {
    return workerProp.getValue();
  }

  @Override
  public Collection<DirectBuffer> fetchVariables() {
    return fetchVariablesProp.stream().map(StringValue::getValue).toList();
  }

  @Override
  public long timeout() {
    return timeoutProp.getValue();
  }

  @Override
  public Collection<String> tenantIds() {
    return tenantIdsProp.stream().map(v -> bufferAsString(v.getValue())).toList();
  }
}
