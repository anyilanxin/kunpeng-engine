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

import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancer.CreateSubchannelArgs;
import io.grpc.LoadBalancer.Helper;
import io.grpc.LoadBalancer.ResolvedAddresses;
import io.grpc.LoadBalancer.Subchannel;
import io.grpc.Status;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Adaptive load balancer: maintains {@link TrackedSubchannel} per backend, tracks READY set, and
 * rebuilds a {@link KunpengSubchannelPicker} (P2C) on every address or state change.
 *
 * <p>Uses the 1.81+ Subchannel API: {@code createSubchannel + start(listener)} rather than the
 * deprecated {@code handleSubchannelState}. The SynchronizationContext guarantees all callbacks are
 * serialized, so no explicit locking is required.
 */
final class KunpengLoadBalancer extends LoadBalancer {

  private final Helper helper;
  private final Map<List<EquivalentAddressGroup>, TrackedSubchannel> subchannels =
      new LinkedHashMap<>();
  private final Map<Subchannel, ConnectivityState> stateBySubchannel = new HashMap<>();

  KunpengLoadBalancer(final Helper helper) {
    this.helper = helper;
  }

  @Override
  public Status acceptResolvedAddresses(final ResolvedAddresses resolvedAddresses) {
    final List<EquivalentAddressGroup> newAddrs = resolvedAddresses.getAddresses();
    final Set<List<EquivalentAddressGroup>> newKeys = new HashSet<>();
    for (final EquivalentAddressGroup eag : newAddrs) {
      newKeys.add(List.of(eag));
    }

    // Remove subchannels that disappeared.
    final Set<Entry<List<EquivalentAddressGroup>, TrackedSubchannel>> old =
        new HashSet<>(subchannels.entrySet());
    for (final Entry<List<EquivalentAddressGroup>, TrackedSubchannel> entry : old) {
      if (!newKeys.contains(entry.getKey())) {
        entry.getValue().subchannel().shutdown();
        subchannels.remove(entry.getKey());
        stateBySubchannel.remove(entry.getValue().subchannel());
      }
    }

    // Add subchannels that are new; refresh gateway-reported activeConnections / online on both new
    // and existing entries so load-poll updates flow through without bouncing connections.
    for (final EquivalentAddressGroup eag : newAddrs) {
      final List<EquivalentAddressGroup> key = List.of(eag);
      final Integer reported =
          eag.getAttributes().get(KunpengLoadBalancerAttributes.ACTIVE_CONNECTIONS);
      final int activeConnections = reported == null ? 0 : reported;
      final Boolean reportedOnline = eag.getAttributes().get(KunpengLoadBalancerAttributes.ONLINE);
      final boolean online = reportedOnline == null || reportedOnline;
      final TrackedSubchannel existing = subchannels.get(key);
      if (existing != null) {
        existing.metrics().setActiveConnections(activeConnections);
        existing.metrics().setOnline(online);
        continue;
      }
      final Subchannel sub =
          helper.createSubchannel(
              CreateSubchannelArgs.newBuilder().setAddresses(List.of(eag)).build());
      final GatewayMetrics metrics = new GatewayMetrics();
      metrics.setActiveConnections(activeConnections);
      metrics.setOnline(online);
      final TrackedSubchannel tracked = new TrackedSubchannel(sub, metrics, addressOf(eag));
      subchannels.put(key, tracked);
      stateBySubchannel.put(sub, ConnectivityState.IDLE);
      sub.start(state -> onStateChange(tracked, state));
      sub.requestConnection();
    }
    rebuildPicker();
    return Status.OK;
  }

  @Override
  public void handleNameResolutionError(final Status error) {
    // Keep the last good picker; gRPC will retry name resolution.
  }

  private void onStateChange(final TrackedSubchannel tracked, final ConnectivityStateInfo info) {
    stateBySubchannel.put(tracked.subchannel(), info.getState());
    if (info.getState() == ConnectivityState.IDLE) {
      tracked.subchannel().requestConnection();
    }
    rebuildPicker();
  }

  private void rebuildPicker() {
    final List<TrackedSubchannel> readyList = new ArrayList<>();
    for (final TrackedSubchannel tracked : subchannels.values()) {
      if (stateBySubchannel.get(tracked.subchannel()) == ConnectivityState.READY
          && tracked.metrics().online()) {
        readyList.add(tracked);
      }
    }
    final KunpengSubchannelPicker picker = new KunpengSubchannelPicker(readyList);
    final ConnectivityState state =
        readyList.isEmpty() ? ConnectivityState.CONNECTING : ConnectivityState.READY;
    helper.updateBalancingState(state, picker);
  }

  @Override
  public void requestConnection() {
    for (final TrackedSubchannel tracked : subchannels.values()) {
      if (stateBySubchannel.get(tracked.subchannel()) == ConnectivityState.IDLE) {
        tracked.subchannel().requestConnection();
      }
    }
  }

  @Override
  public void shutdown() {
    for (final TrackedSubchannel tracked : new ArrayList<>(subchannels.values())) {
      tracked.subchannel().shutdown();
    }
    subchannels.clear();
    stateBySubchannel.clear();
  }

  // Test-only accessors
  Map<List<EquivalentAddressGroup>, TrackedSubchannel> subchannels() {
    return subchannels;
  }

  private static String addressOf(final EquivalentAddressGroup eag) {
    final List<SocketAddress> addresses = eag.getAddresses();
    if (addresses == null || addresses.isEmpty()) {
      return "unknown";
    }
    return addresses.getFirst().toString();
  }
}
