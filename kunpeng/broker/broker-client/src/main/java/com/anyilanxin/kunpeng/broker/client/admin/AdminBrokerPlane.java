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

import com.anyilanxin.kunpeng.broker.client.core.ApiResponseReader;
import com.anyilanxin.kunpeng.broker.client.core.BrokerException;
import com.anyilanxin.kunpeng.broker.client.core.BrokerPlane;
import com.anyilanxin.kunpeng.broker.client.core.ClientRequest;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.api.ResponseRecordValue;
import org.agrona.DirectBuffer;

/**
 * 管理面传输策略：固定寻址集群 leader 的管理分区，并按管理面编号池物化响应记录。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AdminBrokerPlane implements BrokerPlane {
  private static final RecordValueMapper VALUE_MAPPER = DefaultRecordValueMapper.getInstance();

  private final ClusterLeaderFoundService leaderFoundService;

  public AdminBrokerPlane(final ClusterLeaderFoundService leaderFoundService) {
    this.leaderFoundService = leaderFoundService;
  }

  @Override
  public Destination resolve(final ClientRequest<?> request) throws BrokerException {
    final Address address = leaderFoundService.getLeaderAddress();
    if (address == null) {
      throw new BrokerException(10, "not found leader");
    }
    return new Destination(TopicUtils.getTopicName(), address);
  }

  @Override
  public ResponseRecordValue materialize(final ApiResponseReader response) {
    if (!response.hasData()) {
      return null;
    }
    final short lifeCycleValue = (short) response.lifeCycle();
    if (AdminValueLifeCycle.UNKNOWN.value() == lifeCycleValue) {
      return null;
    }
    final AdminValueType valueType = AdminValueType.valueOf((short) response.valueType());
    final AdminValueLifeCycle lifeCycle =
        AdminValueLifeCycle.fromProtocolValue(valueType, lifeCycleValue);
    final ResponseRecordValue value = (ResponseRecordValue) VALUE_MAPPER.getValue(lifeCycle);
    final DirectBuffer data = response.data();
    value.wrap(data, 0, data.capacity());
    return value;
  }
}
