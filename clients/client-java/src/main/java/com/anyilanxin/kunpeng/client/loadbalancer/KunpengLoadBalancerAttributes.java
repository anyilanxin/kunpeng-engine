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

import io.grpc.Attributes;

/**
 * Shared {@link Attributes} keys flowing from the name resolvers to {@link KunpengLoadBalancer} via
 * {@link io.grpc.EquivalentAddressGroup#getAttributes()}.
 */
public final class KunpengLoadBalancerAttributes {
  private KunpengLoadBalancerAttributes() {}

  /**
   * Per-gateway active client-connection count, as reported by the gateway's {@code
   * QueryGatewayLoad} RPC. Absent means the first load query has not completed yet; the load
   * balancer treats this as zero.
   */
  public static final Attributes.Key<Integer> ACTIVE_CONNECTIONS =
      Attributes.Key.create("kunpeng-activeConnections");

  /**
   * Whether the gateway answered its load query successfully. Absent means {@code true} (optimistic
   * initial state). When {@code false}, the picker skips this subchannel until the next successful
   * load query flips it back.
   */
  public static final Attributes.Key<Boolean> ONLINE = Attributes.Key.create("kunpeng-online");
}
