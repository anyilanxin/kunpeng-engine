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
package com.anyilanxin.kunpeng.cluster.cluster.messaging;

import com.google.common.collect.Maps;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import java.util.Map;

/** Test unicast service factory. */
public class TestUnicastServiceFactory {
  private final Map<Address, TestUnicastService> services = Maps.newConcurrentMap();

  /**
   * Partitions the service at the given address.
   *
   * @param address the address of the service to partition
   */
  public void partition(final Address address) {
    final TestUnicastService service = services.get(address);
    services.values().stream()
        .filter(s -> !s.address().equals(address))
        .forEach(
            s -> {
              service.partition(s.address());
              s.partition(service.address());
            });
  }

  /**
   * Heals a partition of the service at the given address.
   *
   * @param address the address of the service to heal
   */
  public void heal(final Address address) {
    final TestUnicastService service = services.get(address);
    services.values().stream()
        .filter(s -> !s.address().equals(address))
        .forEach(
            s -> {
              service.heal(s.address());
              s.heal(service.address());
            });
  }

  /**
   * Creates a bi-directional partition between two services.
   *
   * @param address1 the first service
   * @param address2 the second service
   */
  public void partition(final Address address1, final Address address2) {
    final TestUnicastService service1 = services.get(address1);
    final TestUnicastService service2 = services.get(address2);
    service1.partition(service2.address());
    service2.partition(service1.address());
  }

  /**
   * Heals a bi-directional partition between two services.
   *
   * @param address1 the first service
   * @param address2 the second service
   */
  public void heal(final Address address1, final Address address2) {
    final TestUnicastService service1 = services.get(address1);
    final TestUnicastService service2 = services.get(address2);
    service1.heal(service2.address());
    service2.heal(service1.address());
  }

  /**
   * Returns a new test unicast service for the given endpoint.
   *
   * @param address the address to which to bind
   * @return the unicast service for the given endpoint
   */
  public ManagedUnicastService newUnicastService(final Address address) {
    return new TestUnicastService(address, services);
  }
}
