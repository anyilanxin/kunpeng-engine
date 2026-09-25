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
package com.anyilanxin.kunpeng.client.loadbalancer;

import java.net.URI;
import java.util.List;

/**
 * Supplies the load balancer with two independent signals: gateway membership (discovery) and
 * per-gateway load (queried directly from each gateway).
 *
 * <p>Splitting these concerns keeps cluster-broadcast size proportional to topology only — load
 * changes no longer trigger re-broadcasts.
 */
public interface GatewayServiceDiscoverAdapter extends AutoCloseable {

  /**
   * Query a known gateway for the current cluster gateway list. Returns URIs only; load is fetched
   * separately via {@link #queryGatewayLoad(URI)}.
   *
   * @return cluster gateway URIs (never {@code null}; may be empty on failure)
   */
  List<URI> discoverGateways();

  /**
   * Query a single gateway for its current load. Implementations should target the gateway at
   * {@code gateway} directly so the call doubles as a liveness probe.
   *
   * @return the gateway's current active-connection count
   * @throws Exception if the gateway is unreachable or the RPC fails — callers must treat this as
   *     "gateway offline"
   */
  int queryGatewayLoad(URI gateway) throws Exception;
}
