/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.utils.net;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.utils.net.AddressInitializations.LocalHostResolver;
import io.netty.util.NetUtil;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

final class AddressInitializationsTest {

  private static final InetAddress SITE_IPV4 =
      NetUtil.createInetAddressFromIpAddressString("10.20.30.40");
  private static final InetAddress SITE_IPV6 =
      NetUtil.createInetAddressFromIpAddressString("fd00::1a2b:3c4d");

  @Test
  void shouldAdvertiseOsLocalhostWhenResolvable() {
    final var osLocalhost = SITE_IPV4;

    final var picked =
        AddressInitializations.computeDefaultAdvertisedHost(
            mixedAddresses(), alwaysResolveTo(osLocalhost), false);

    assertThat(picked).isSameAs(osLocalhost);
  }

  @Test
  void shouldPickFirstNonLoopbackIpv4WhenPreferred() {
    final var picked =
        AddressInitializations.computeDefaultAdvertisedHost(
            mixedAddresses(), unresolvableHost(), false);

    assertThat(picked).isEqualTo(SITE_IPV4);
  }

  @Test
  void shouldPickFirstNonLoopbackIpv6WhenPreferred() {
    final var picked =
        AddressInitializations.computeDefaultAdvertisedHost(
            mixedAddresses(), unresolvableHost(), true);

    assertThat(picked).isEqualTo(SITE_IPV6);
  }

  @Test
  void shouldFallBackToOtherFamilyWhenPreferredOneIsAbsent() {
    final var onlyIpv6 = Stream.of(NetUtil.LOCALHOST4, SITE_IPV6);

    final var picked =
        AddressInitializations.computeDefaultAdvertisedHost(onlyIpv6, unresolvableHost(), false);

    assertThat(picked).isEqualTo(SITE_IPV6);
  }

  @Test
  void shouldFallBackToIpv4LoopbackWithoutSiteAddresses() {
    final var picked =
        AddressInitializations.computeDefaultAdvertisedHost(
            Stream.of(NetUtil.LOCALHOST4, NetUtil.LOCALHOST6), unresolvableHost(), false);

    assertThat(picked).isEqualTo(NetUtil.LOCALHOST4);
  }

  @Test
  void shouldFallBackToIpv6LoopbackWithoutSiteAddresses() {
    final var picked =
        AddressInitializations.computeDefaultAdvertisedHost(
            Stream.of(NetUtil.LOCALHOST4, NetUtil.LOCALHOST6), unresolvableHost(), true);

    assertThat(picked).isEqualTo(NetUtil.LOCALHOST6);
  }

  /** 环回地址与站点地址交替出现，验证过滤环回与"取首个"的语义。 */
  private Stream<InetAddress> mixedAddresses() {
    return Stream.of(NetUtil.LOCALHOST6, SITE_IPV4, NetUtil.LOCALHOST4, SITE_IPV6);
  }

  private LocalHostResolver alwaysResolveTo(final InetAddress address) {
    return () -> address;
  }

  private LocalHostResolver unresolvableHost() {
    return () -> {
      throw new UnknownHostException("no local host in this environment");
    };
  }
}
