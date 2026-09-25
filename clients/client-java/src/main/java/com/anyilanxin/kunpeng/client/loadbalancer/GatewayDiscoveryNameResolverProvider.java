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

import io.grpc.NameResolver;
import io.grpc.NameResolver.Args;
import io.grpc.NameResolverProvider;
import java.net.URI;
import java.time.Duration;

public class GatewayDiscoveryNameResolverProvider extends NameResolverProvider {
  private final GatewayServiceDiscoverAdapter addressAdapter;
  public static final String GATEWAY_DISCOVER_SCHEME = "gateway";
  public static final String GATEWAY_DISCOVER_TARGET =
      GATEWAY_DISCOVER_SCHEME + "://kunpeng-gateway-discover";
  private final Duration gatewayDiscover;
  private final Duration gatewayLoadQuery;

  public GatewayDiscoveryNameResolverProvider(
      final GatewayServiceDiscoverAdapter addressAdapter,
      final Duration gatewayDiscover,
      final Duration gatewayLoadQuery) {
    this.addressAdapter = addressAdapter;
    this.gatewayDiscover = gatewayDiscover;
    this.gatewayLoadQuery = gatewayLoadQuery;
  }

  @Override
  protected boolean isAvailable() {
    return true;
  }

  @Override
  protected int priority() {
    return 4;
  }

  @Override
  public NameResolver newNameResolver(final URI uri, final Args args) {
    return new GatewayDiscoveryNameResolver(
        uri, args, addressAdapter, gatewayDiscover, gatewayLoadQuery);
  }

  @Override
  public String getDefaultScheme() {
    return GATEWAY_DISCOVER_SCHEME;
  }
}
