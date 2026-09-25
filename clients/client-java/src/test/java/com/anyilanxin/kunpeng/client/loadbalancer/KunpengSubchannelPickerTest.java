/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.client.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer.PickResult;
import io.grpc.LoadBalancer.PickSubchannelArgs;
import io.grpc.LoadBalancer.Subchannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link KunpengSubchannelPicker} P2C selection logic with stub subchannels.
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class KunpengSubchannelPickerTest {

  @Test
  void emptyReadyListReturnsNoResult() {
    final KunpengSubchannelPicker picker = new KunpengSubchannelPicker(List.of());
    final PickResult result = picker.pickSubchannel(args());
    assertThat(result).isSameAs(PickResult.withNoResult());
  }

  @Test
  void singleReadyAlwaysPicked() {
    final StubSubchannel sub = new StubSubchannel();
    final TrackedSubchannel tracked = new TrackedSubchannel(sub, new GatewayMetrics(), "host1:2024");
    final KunpengSubchannelPicker picker = new KunpengSubchannelPicker(List.of(tracked));
    for (int i = 0; i < 50; i++) {
      final PickResult r = picker.pickSubchannel(args());
      assertThat(r.getSubchannel()).isSameAs(sub);
    }
  }

  @Test
  void p2cBiasesTowardLessLoadedSubchannel() {
    final StubSubchannel light = new StubSubchannel();
    final StubSubchannel heavy = new StubSubchannel();
    final GatewayMetrics lightMetrics = new GatewayMetrics();
    final GatewayMetrics heavyMetrics = new GatewayMetrics();
    // Dynamic-discovery style load: gateway-reported active connections.
    lightMetrics.setActiveConnections(0);
    heavyMetrics.setActiveConnections(20);
    final List<TrackedSubchannel> ready =
        List.of(
            new TrackedSubchannel(light, lightMetrics, "light:2024"),
            new TrackedSubchannel(heavy, heavyMetrics, "heavy:2024"));

    final KunpengSubchannelPicker picker = new KunpengSubchannelPicker(ready);
    final Map<Subchannel, Integer> counts = new HashMap<>();
    for (int i = 0; i < 500; i++) {
      final PickResult r = picker.pickSubchannel(args());
      counts.merge(r.getSubchannel(), 1, Integer::sum);
    }
    // P2C with 2 candidates picks heavy only when both random draws land on it (~25%).
    // Expected ratio is ~3:1, so assert light gets at least 2x heavy's count.
    assertThat(counts.get(light)).isGreaterThan(counts.get(heavy) * 2);
  }

  @Test
  void p2cBiasesTowardLowerLatencyWhenLoadIsEqual() {
    final StubSubchannel fast = new StubSubchannel();
    final StubSubchannel slow = new StubSubchannel();
    final GatewayMetrics fastMetrics = new GatewayMetrics();
    final GatewayMetrics slowMetrics = new GatewayMetrics();
    // Both gateways report zero active connections; latency becomes the tiebreaker.
    for (int i = 0; i < 5; i++) {
      slowMetrics.recordStream(System.nanoTime() - 1_000_000_000L, Status.OK);
    }
    final List<TrackedSubchannel> ready =
        List.of(
            new TrackedSubchannel(fast, fastMetrics, "fast:2024"),
            new TrackedSubchannel(slow, slowMetrics, "slow:2024"));

    final KunpengSubchannelPicker picker = new KunpengSubchannelPicker(ready);
    final Map<Subchannel, Integer> counts = new HashMap<>();
    for (int i = 0; i < 500; i++) {
      final PickResult r = picker.pickSubchannel(args());
      counts.merge(r.getSubchannel(), 1, Integer::sum);
    }
    assertThat(counts.get(fast)).isGreaterThan(counts.get(slow) * 2);
  }

  private static PickSubchannelArgs args() {
    return new PickSubchannelArgs() {
      @Override
      public MethodDescriptor<?, ?> getMethodDescriptor() {
        return null;
      }

      @Override
      public Metadata getHeaders() {
        return new Metadata();
      }

      @Override
      public io.grpc.CallOptions getCallOptions() {
        return io.grpc.CallOptions.DEFAULT;
      }
    };
  }

  /** Minimal Subchannel stub — only getSubchannel() in PickResult needs to return same instance. */
  private static final class StubSubchannel extends Subchannel {
    @Override
    public void shutdown() {}

    @Override
    public void requestConnection() {}

    @Override
    public List<EquivalentAddressGroup> getAllAddresses() {
      return new ArrayList<>();
    }

    @Override
    public Attributes getAttributes() {
      return Attributes.EMPTY;
    }
  }
}
