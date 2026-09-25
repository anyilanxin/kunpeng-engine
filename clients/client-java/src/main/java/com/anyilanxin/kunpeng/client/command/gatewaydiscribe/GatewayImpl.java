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
import java.util.List;
import java.util.stream.Collectors;

public final class GatewayImpl implements Gateway {

  private final List<GatewayInfo> gateways;

  public GatewayImpl(final ClusterManageServiceOuterClass.QueryGatewayResponse grpcResponse) {
    gateways =
        grpcResponse.getGatewaysList().stream()
            .map(GatewayInfoImpl::new)
            .collect(Collectors.toList());
  }

  @Override
  public List<GatewayInfo> getGateways() {
    return gateways;
  }

  @Override
  public String toString() {
    return "GatewayImpl{" + "gateways=" + gateways + '}';
  }
}
