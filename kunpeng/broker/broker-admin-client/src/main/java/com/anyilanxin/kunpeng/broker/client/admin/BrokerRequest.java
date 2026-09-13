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
package com.anyilanxin.kunpeng.broker.client.admin;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.AdminApiRequestEncoder;
import com.anyilanxin.kunpeng.protocol.common.api.RequestRecordValue;

public abstract class BrokerRequest<VALUE extends RequestRecordValue>
    implements ClientRequest<VALUE> {
  private long key = AdminApiRequestEncoder.keyNullValue();
  private final AdminValueType valueType;
  private final AdminValueLifeCycle lifeCycle;
  private final VALUE value;

  public BrokerRequest(
      final AdminValueType valueType, final AdminValueLifeCycle lifeCycle, final VALUE value) {
    this.valueType = valueType;
    this.lifeCycle = lifeCycle;
    this.value = value;
  }

  @Override
  public VALUE getValue() {
    return value;
  }

  @Override
  public void setKey(final long key) {
    this.key = key;
  }

  @Override
  public long key() {
    return key;
  }

  @Override
  public AdminValueType valueType() {
    return valueType;
  }

  @Override
  public AdminValueLifeCycle lifeCycle() {
    return lifeCycle;
  }
}
