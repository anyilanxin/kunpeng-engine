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

import com.anyilanxin.kunpeng.client.ClientLoggers;
import io.grpc.LoadBalancer.PickResult;
import io.grpc.LoadBalancer.PickSubchannelArgs;
import io.grpc.LoadBalancer.Subchannel;
import io.grpc.LoadBalancer.SubchannelPicker;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;

/**
 * Power-of-two-choices picker: pick two random READY subchannels, return the one with higher {@link
 * GatewayMetrics#score()}. Each pick attaches a fresh {@link MetricsStreamTracerFactory} bound to
 * the chosen subchannel's metrics, so the next pick sees updated load.
 */
final class KunpengSubchannelPicker extends SubchannelPicker {
  static final String NAME = "kunpengP2C";
  private static final Logger LOG = ClientLoggers.LOGGER;

  private final List<TrackedSubchannel> readyList;

  KunpengSubchannelPicker(final List<TrackedSubchannel> readyList) {
    this.readyList = readyList;
  }

  @Override
  public PickResult pickSubchannel(final PickSubchannelArgs args) {
    final int size = readyList.size();
    if (size == 0) {
      return PickResult.withNoResult();
    }
    final TrackedSubchannel chosen;
    if (size == 1) {
      chosen = readyList.getFirst();
    } else {
      final ThreadLocalRandom rng = ThreadLocalRandom.current();
      final TrackedSubchannel a = readyList.get(rng.nextInt(size));
      final TrackedSubchannel b = readyList.get(rng.nextInt(size));
      chosen = a.metrics().score() >= b.metrics().score() ? a : b;
    }
    LOG.debug("Routing request to gateway {}", chosen.address());
    return withTracer(chosen);
  }

  private static PickResult withTracer(final TrackedSubchannel tracked) {
    final Subchannel subchannel = tracked.subchannel();
    final var tracerFactory = new MetricsStreamTracerFactory(tracked.metrics());
    return PickResult.withSubchannel(subchannel, tracerFactory);
  }
}
