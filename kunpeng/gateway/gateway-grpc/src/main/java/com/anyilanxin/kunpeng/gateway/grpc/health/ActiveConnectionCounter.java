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
package com.anyilanxin.kunpeng.gateway.grpc.health;

import io.grpc.Metadata;
import io.grpc.ServerStreamTracer;
import io.grpc.Status;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts active RPC streams on the gateway. Each long-lived client TCP/HTTP2 connection carries one
 * or more concurrent RPC streams; the total stream count across all clients is the most direct
 * indicator of gateway load — better than raw TCP connection count, because a connection with zero
 * active streams contributes nothing to load.
 *
 * <p>Registered via {@code ServerBuilder.addStreamTracerFactory(...)}; the count is published to
 * the cluster through {@link
 * com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover.GatewayTopologyManagerImpl} so that clients
 * performing dynamic gateway discovery can prefer less-loaded gateways.
 */
public final class ActiveConnectionCounter extends ServerStreamTracer.Factory {
  private final AtomicInteger activeStreams = new AtomicInteger(0);

  @Override
  public ServerStreamTracer newServerStreamTracer(
      final String fullMethodName, final Metadata headers) {
    activeStreams.incrementAndGet();
    return new ServerStreamTracer() {
      @Override
      public void streamClosed(final Status status) {
        activeStreams.decrementAndGet();
      }
    };
  }

  public int getActiveConnections() {
    return activeStreams.get();
  }
}
