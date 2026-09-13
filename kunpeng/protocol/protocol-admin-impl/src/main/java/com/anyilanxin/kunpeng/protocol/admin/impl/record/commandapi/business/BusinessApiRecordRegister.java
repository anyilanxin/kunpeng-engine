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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.dispatch.*;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.query.BusinessDispatchQueryRequestRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.query.BusinessDispatchQueryResponseRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordValueMapperRegister;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.business.BusinessDispatchApiValueLifeCycle;

/**
 * @author zxuanhong
 * @since
 */
public class BusinessApiRecordRegister {
  private BusinessApiRecordRegister() {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void register(final RecordValueMapperRegister valueMapper) {
    valueMapper
        .register(
            BusinessDispatchApiValueLifeCycle.CLUSTER_BALANCE_REQUEST,
            BusinessClusterBalanceRequestRecord::new)
        .register(
            BusinessDispatchApiValueLifeCycle.CLUSTER_BALANCE_RESPONSE,
            BusinessChangeResponseRecord::new)
        .register(
            BusinessDispatchApiValueLifeCycle.CHANGE_PARTITION_REQUEST,
            BusinessChangePartitionRequestRecord::new)
        .register(
            BusinessDispatchApiValueLifeCycle.CHANGE_PARTITION_RESPONSE,
            BusinessChangeResponseRecord::new)
        .register(
            BusinessDispatchApiValueLifeCycle.CHANGE_REPLICATION_REQUEST,
            BusinessChangeReplicationRequestRecord::new)
        .register(
            BusinessDispatchApiValueLifeCycle.CHANGE_REPLICATION_RESPONSE,
            BusinessChangeResponseRecord::new)
        .register(
            BusinessDispatchApiValueLifeCycle.DISPATCH_QUERY_REQUEST,
            BusinessDispatchQueryRequestRecord::new)
        .register(
            BusinessDispatchApiValueLifeCycle.DISPATCH_QUERY_RESPONSE,
            BusinessDispatchQueryResponseRecord::new)
        .register(
            BusinessDispatchApiValueLifeCycle.CHANGE_CANCEL_REQUEST,
            BusinessChangeCancelRequestRecord::new)
        .register(
            BusinessDispatchApiValueLifeCycle.CHANGE_CANCEL_RESPONSE,
            BusinessDispatchQueryResponseRecord::new);
  }
}
