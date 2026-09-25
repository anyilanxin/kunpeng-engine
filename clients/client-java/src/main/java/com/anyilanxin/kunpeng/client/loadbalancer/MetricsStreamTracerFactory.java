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

import io.grpc.ClientStreamTracer;
import io.grpc.ClientStreamTracer.StreamInfo;
import io.grpc.Metadata;
import io.grpc.Status;

/**
 * Per-pick {@link ClientStreamTracer.Factory} that records one {@link
 * GatewayMetrics#recordStream(long, Status)} entry per stream completion. A fresh factory is
 * created on every {@code pickSubchannel} call, closing over the chosen subchannel's metrics
 * instance.
 */
final class MetricsStreamTracerFactory extends ClientStreamTracer.Factory {

  private final GatewayMetrics metrics;

  MetricsStreamTracerFactory(final GatewayMetrics metrics) {
    this.metrics = metrics;
  }

  @Override
  public ClientStreamTracer newClientStreamTracer(final StreamInfo info, final Metadata headers) {
    final long startNano = System.nanoTime();
    return new ClientStreamTracer() {
      @Override
      public void streamClosed(final Status status) {
        metrics.recordStream(startNano, status);
      }
    };
  }
}
