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
package io.atomix.utils.net;

import io.atomix.utils.VisibleForTesting;
import io.netty.util.NetUtil;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.stream.Stream;

/**
 * 计算默认广播地址（advertised host），选取顺序：
 *
 * <ol>
 *   <li>操作系统解析的 localhost（通常由主机名解析而来）；
 *   <li>解析失败时，从网卡地址中各取首个非环回的 IPv4 / IPv6 地址，按系统地址族偏好取首选，
 *       首选族缺失时取另一族；
 *   <li>全部缺失时退回偏好族的环回地址。
 * </ol>
 */
final class AddressInitializations {

  private AddressInitializations() {}

  static InetAddress computeDefaultAdvertisedHost() {
    return computeDefaultAdvertisedHost(
        NetUtil.NETWORK_INTERFACES.stream().flatMap(NetworkInterface::inetAddresses),
        InetAddress::getLocalHost,
        NetUtil.isIpV6AddressesPreferred());
  }

  @VisibleForTesting
  static InetAddress computeDefaultAdvertisedHost(
      final Stream<InetAddress> interfaceAddresses,
      final LocalHostResolver localHostResolver,
      final boolean ipv6Preferred) {
    try {
      return localHostResolver.resolve();
    } catch (final UnknownHostException hostUnresolvable) {
      // 主机名解析不可用（常见于容器环境），继续用网卡地址推断
    }

    final InetAddress[] firstPerFamily = firstNonLoopbackPerFamily(interfaceAddresses);
    final InetAddress preferredFamily = firstPerFamily[ipv6Preferred ? 1 : 0];
    if (preferredFamily != null) {
      return preferredFamily;
    }

    final InetAddress otherFamily = firstPerFamily[ipv6Preferred ? 0 : 1];
    if (otherFamily != null) {
      return otherFamily;
    }

    return ipv6Preferred ? NetUtil.LOCALHOST6 : NetUtil.LOCALHOST4;
  }

  /** 扫描网卡地址，返回 {@code [首个非环回 IPv4, 首个非环回 IPv6]}，对应族缺失时为 null。 */
  private static InetAddress[] firstNonLoopbackPerFamily(final Stream<InetAddress> interfaceAddresses) {
    final InetAddress[] firstPerFamily = new InetAddress[2];
    interfaceAddresses
        .filter(address -> !address.isLoopbackAddress())
        .forEach(
            address -> {
              final int familyIndex = address instanceof Inet6Address ? 1 : 0;
              if (firstPerFamily[familyIndex] == null) {
                firstPerFamily[familyIndex] = address;
              }
            });
    return firstPerFamily;
  }

  /** localhost 解析器，单独抽象以便测试注入替身。 */
  @VisibleForTesting
  @FunctionalInterface
  interface LocalHostResolver {
    InetAddress resolve() throws UnknownHostException;
  }
}
