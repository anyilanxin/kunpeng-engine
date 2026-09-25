/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.client.command.topology;

import com.anyilanxin.kunpeng.client.command.BrokerInfo;
import com.anyilanxin.kunpeng.client.command.BrokerInfoImpl;
import com.anyilanxin.kunpeng.gateway.grpc.service.ClusterManageServiceOuterClass;
import java.util.List;
import java.util.stream.Collectors;

public final class TopologyCommandResponseImpl implements TopologyCommandResponse {

  private final List<BrokerInfo> brokers;
  private final int clusterSize;
  private final int partitionsCount;
  private final int replicationFactor;
  private final String gatewayVersion;

  public TopologyCommandResponseImpl(
      final ClusterManageServiceOuterClass.ClusterTopologyResponse grpcResponse) {
    brokers =
        grpcResponse.getBrokersList().stream()
            .map(BrokerInfoImpl::new)
            .collect(Collectors.toList());
    clusterSize = grpcResponse.getClusterSize();
    partitionsCount = grpcResponse.getPartitionsCount();
    replicationFactor = grpcResponse.getReplicationFactor();
    gatewayVersion = grpcResponse.getGatewayVersion();
  }

  @Override
  public List<BrokerInfo> getBrokers() {
    return brokers;
  }

  @Override
  public int getClusterSize() {
    return clusterSize;
  }

  @Override
  public int getPartitionsCount() {
    return partitionsCount;
  }

  @Override
  public int getReplicationFactor() {
    return replicationFactor;
  }

  @Override
  public String getGatewayVersion() {
    return gatewayVersion;
  }

  @Override
  public String toString() {
    return "TopologyCommandResponseImpl{"
        + "brokers="
        + brokers
        + ", clusterSize="
        + clusterSize
        + ", partitionsCount="
        + partitionsCount
        + ", replicationFactor="
        + replicationFactor
        + ", gatewayVersion="
        + gatewayVersion
        + '}';
  }
}
