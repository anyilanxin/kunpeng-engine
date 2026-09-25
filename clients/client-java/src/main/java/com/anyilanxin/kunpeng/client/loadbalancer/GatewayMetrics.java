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

import io.grpc.Status;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-subchannel metrics tracked client-side to drive the P2C load balancer.
 *
 * <p>Two signals drive the score: gateway-reported {@code activeConnections} (queried via the
 * gateway load RPC) and client-observed latency EWMA (from {@link MetricsStreamTracerFactory}). A
 * 5-second error penalty applies after a failed stream.
 *
 * <p>An {@code online} flag mirrors the result of the latest gateway load RPC: {@code false} means
 * the gateway failed to answer the load query and must be skipped by the picker even if its
 * subchannel reports READY.
 */
final class GatewayMetrics {
  private static final double EWMA_ALPHA = 0.3;
  private static final double INITIAL_LATENCY_MS = 50.0;
  private static final long ERROR_PENALTY_WINDOW_MS = 5_000L;
  private static final double ERROR_PENALTY_FACTOR = 0.1;

  private volatile int activeConnections = 0;
  private volatile boolean online = true;
  private volatile double latencyEwmaMs = INITIAL_LATENCY_MS;
  private final AtomicLong lastErrorEpochMilli = new AtomicLong(0L);

  /** Called by {@link KunpengLoadBalancer} when a fresh {@code activeConnections} value arrives. */
  void setActiveConnections(final int value) {
    activeConnections = value;
  }

  /** Called by {@link KunpengLoadBalancer} when the gateway's online status changes. */
  void setOnline(final boolean value) {
    online = value;
  }

  boolean online() {
    return online;
  }

  /** Called by {@link MetricsStreamTracerFactory} when a stream completes. */
  void recordStream(final long startNano, final Status status) {
    final long durationMs = Math.max(1L, (System.nanoTime() - startNano) / 1_000_000L);
    synchronized (this) {
      latencyEwmaMs = latencyEwmaMs + EWMA_ALPHA * (durationMs - latencyEwmaMs);
    }
    if (!status.isOk()) {
      lastErrorEpochMilli.set(System.currentTimeMillis());
    }
  }

  /**
   * Higher is better. Base score is {@code 1 / (activeConnections + 1) / latency}; a 5-second error
   * window applies a 0.1x penalty.
   */
  double score() {
    final long now = System.currentTimeMillis();
    final long lastErr = lastErrorEpochMilli.get();
    double penalty = 1.0;
    if (lastErr > 0 && (now - lastErr) < ERROR_PENALTY_WINDOW_MS) {
      penalty = ERROR_PENALTY_FACTOR;
    }
    final int ac = activeConnections;
    final double latency = Math.max(1.0, latencyEwmaMs);
    return penalty * (1.0 / (ac + 1.0)) * (1.0 / latency);
  }

  int activeConnections() {
    return activeConnections;
  }

  double latencyEwmaMs() {
    return latencyEwmaMs;
  }
}
