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

import io.grpc.LoadBalancer;
import io.grpc.LoadBalancer.Helper;
import io.grpc.LoadBalancerProvider;

/**
 * SPI entry point for the kunpeng adaptive (P2C) load balancer. Referenced by name via {@code
 * channelBuilder.defaultLoadBalancingPolicy(KunpengLoadBalancerProvider.POLICY_NAME)}.
 */
public final class KunpengLoadBalancerProvider extends LoadBalancerProvider {

  public static final String POLICY_NAME = "kunpeng-p2c";

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public int getPriority() {
    return 6;
  }

  @Override
  public String getPolicyName() {
    return POLICY_NAME;
  }

  @Override
  public LoadBalancer newLoadBalancer(final Helper helper) {
    return new KunpengLoadBalancer(helper);
  }
}
