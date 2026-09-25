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
package com.anyilanxin.kunpeng.client.command.gatewaydiscribe;

import com.anyilanxin.kunpeng.gateway.grpc.service.ClusterManageServiceOuterClass;
import java.util.Objects;

public final class GatewayInfoImpl implements GatewayInfo {
  private final String nodeId;
  private final String gatewayAddress;
  private final String grpcAddress;
  private final String version;
  private final boolean allowClientDiscovery;

  public GatewayInfoImpl(final ClusterManageServiceOuterClass.GatewayInfo grpcGatewayInfo) {
    nodeId = grpcGatewayInfo.getNodeId();
    gatewayAddress = grpcGatewayInfo.getGatewayAddress();
    grpcAddress = grpcGatewayInfo.getGrpcAddress();
    version = grpcGatewayInfo.getVersion();
    allowClientDiscovery = grpcGatewayInfo.getAllowClientDiscovery();
  }

  @Override
  public String getNodeId() {
    return nodeId;
  }

  @Override
  public String getGatewayAddress() {
    return gatewayAddress;
  }

  @Override
  public String getGrpcAddress() {
    return grpcAddress;
  }

  @Override
  public boolean isAllowClientDiscovery() {
    return allowClientDiscovery;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeId, gatewayAddress, grpcAddress, version, allowClientDiscovery);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final GatewayInfoImpl that = (GatewayInfoImpl) o;
    return allowClientDiscovery == that.allowClientDiscovery
        && Objects.equals(nodeId, that.nodeId)
        && Objects.equals(grpcAddress, that.grpcAddress)
        && Objects.equals(gatewayAddress, that.gatewayAddress)
        && Objects.equals(version, that.version);
  }

  @Override
  public String toString() {
    return "BrokerInfoImpl{"
        + "nodeId="
        + nodeId
        + ", grpcAddress="
        + grpcAddress
        + ", gatewayAddress="
        + gatewayAddress
        + ", allowClientDiscovery="
        + allowClientDiscovery
        + ", version="
        + version
        + '}';
  }
}
