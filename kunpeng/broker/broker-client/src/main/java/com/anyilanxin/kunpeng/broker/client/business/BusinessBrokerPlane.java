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
package com.anyilanxin.kunpeng.broker.client.business;

import static com.anyilanxin.kunpeng.broker.client.core.BrokerRequest.NOT_SPECIFIED_PARTITION;
import static com.anyilanxin.kunpeng.broker.client.core.BrokerRequest.RANDOM_PARTITION;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.BUSINESS_RAFT_GROUP;

import com.anyilanxin.kunpeng.broker.client.core.ApiResponseReader;
import com.anyilanxin.kunpeng.broker.client.core.BrokerException;
import com.anyilanxin.kunpeng.broker.client.core.BrokerPlane;
import com.anyilanxin.kunpeng.broker.client.core.BrokerRequest;
import com.anyilanxin.kunpeng.broker.client.core.protocol.RecordType;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.Protocol;
import com.anyilanxin.kunpeng.protocol.common.api.ResponseRecordValue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.agrona.DirectBuffer;

/**
 * 业务面传输策略：按请求类别寻址业务分区 leader，并按业务面编号池物化响应记录。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BusinessBrokerPlane implements BrokerPlane {
  private static final RecordValueMapper VALUE_MAPPER = DefaultRecordValueMapper.getInstance();

  private final ClusterTopologyService topologyService;

  public BusinessBrokerPlane(final ClusterTopologyService topologyService) {
    this.topologyService = topologyService;
  }

  @Override
  public Destination resolve(final BrokerRequest<?> request) throws BrokerException {
    return switch (RecordType.get((short) request.requestType())) {
      case COMMAND_API -> commandApiDestination(request);
      case QUERY -> queryDestination(request);
      default -> throw new BrokerException(10, "Unexpected request type");
    };
  }

  @Override
  public ResponseRecordValue materialize(final ApiResponseReader response) {
    if (!response.hasData()) {
      return null;
    }
    final short lifeCycleValue = (short) response.lifeCycle();
    if (ValueLifeCycle.UNKNOWN.value() == lifeCycleValue) {
      return null;
    }
    final ValueType valueType = ValueType.valueOf((short) response.valueType());
    final ValueLifeCycle lifeCycle = ValueLifeCycle.fromProtocolValue(valueType, lifeCycleValue);
    final ResponseRecordValue value = (ResponseRecordValue) VALUE_MAPPER.getValue(lifeCycle);
    final DirectBuffer data = response.data();
    value.wrap(data, 0, data.capacity());
    return value;
  }

  private Destination commandApiDestination(final BrokerRequest<?> request) throws BrokerException {
    final int partitionId = request.partitionId();
    if (partitionId == RANDOM_PARTITION) {
      final PartitionId partition = randomPartition();
      if (partition == null) {
        throw new BrokerException(10, "No business partition with a leader is available");
      }
      return partitionDestination(partition);
    } else if (partitionId == NOT_SPECIFIED_PARTITION) {
      final long key = request.key();
      if (key == -1) {
        throw new BrokerException(10, "Invalid key or no target partition specified");
      }
      final int sourceId = Protocol.decodeResourceId(key);
      final PartitionId partition = topologyService.getPartitionBySourceId(sourceId);
      if (partition == null) {
        throw new BrokerException(10, "No partition found for source " + sourceId);
      }
      return partitionDestination(partition);
    }
    return partitionDestination(PartitionId.from(BUSINESS_RAFT_GROUP, partitionId));
  }

  private Destination queryDestination(final BrokerRequest<?> request) throws BrokerException {
    final int partitionId = request.partitionId();
    if (partitionId <= 0) {
      throw new BrokerException(10, "No valid partition specified");
    }
    return partitionDestination(PartitionId.from(BUSINESS_RAFT_GROUP, partitionId));
  }

  private Destination partitionDestination(final PartitionId partitionId) throws BrokerException {
    // 业务面寻址统一走拓扑收集服务(分区 leader 的成员主机 + 其广播的业务端口);
    // 地址不可得(leader 未选出/成员离线/业务端口未广播)本身就是异常, 立即抛出让调用方拿到明确失败
    final Address businessAddress = topologyService.getPartitionBusinessAddress(partitionId);
    if (businessAddress == null) {
      throw new BrokerException(
          10,
          "Business address of partition leader not resolvable for partition "
              + partitionId
              + " (leader unknown, member offline, or business port not broadcast)");
    }
    return new Destination(
        TopicUtils.getTopicName(RecordType.COMMAND_API.value(), String.valueOf(partitionId.id())),
        businessAddress);
  }

  private PartitionId randomPartition() {
    final List<PartitionId> candidates = new ArrayList<>();
    for (final PartitionId partitionId :
        topologyService.getRaftGroup(BUSINESS_RAFT_GROUP).keySet()) {
      if (topologyService.getPartitionLeader(partitionId) != null) {
        candidates.add(partitionId);
      }
    }
    if (candidates.isEmpty()) {
      return null;
    }
    return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
  }
}
