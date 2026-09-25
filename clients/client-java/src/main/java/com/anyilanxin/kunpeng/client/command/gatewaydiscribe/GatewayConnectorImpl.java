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

public final class GatewayConnectorImpl implements GatewayConnectorInfo {
  private final String nodeId;
  private final int tActiveConnections;

  public GatewayConnectorImpl(
      final ClusterManageServiceOuterClass.QueryGatewayLoadResponse grpcResponse) {
    nodeId = grpcResponse.getNodeId();
    tActiveConnections = grpcResponse.getActiveConnections();
  }

  @Override
  public String getNodeId() {
    return "";
  }

  @Override
  public int getActiveConnections() {
    return 0;
  }
}
