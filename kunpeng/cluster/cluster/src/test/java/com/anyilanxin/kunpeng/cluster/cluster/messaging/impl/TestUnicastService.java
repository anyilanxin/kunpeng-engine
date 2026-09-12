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
package com.anyilanxin.kunpeng.cluster.cluster.messaging.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ManagedUnicastService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.UnicastService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/** Test unicast service. */
public class TestUnicastService implements ManagedUnicastService {
  private final Address address;
  private final Map<Address, TestUnicastService> services;
  private final Map<String, Map<BiConsumer<Address, byte[]>, Executor>> listeners =
      Maps.newConcurrentMap();
  private final AtomicBoolean started = new AtomicBoolean();
  private final Set<Address> partitions = Sets.newConcurrentHashSet();

  public TestUnicastService(
      final Address address, final Map<Address, TestUnicastService> services) {
    this.address = address;
    this.services = services;
  }

  /**
   * Returns the service address.
   *
   * @return the service address
   */
  Address address() {
    return address;
  }

  /** Partitions the node from the given address. */
  void partition(final Address address) {
    partitions.add(address);
  }

  /** Heals the partition from the given address. */
  void heal(final Address address) {
    partitions.remove(address);
  }

  /**
   * Returns a boolean indicating whether this node is partitioned from the given address.
   *
   * @param address the address to check
   * @return whether this node is partitioned from the given address
   */
  boolean isPartitioned(final Address address) {
    return partitions.contains(address);
  }

  @Override
  public void unicast(final Address address, final String subject, final byte[] message) {
    if (isPartitioned(address)) {
      return;
    }

    final TestUnicastService service = services.get(address);
    if (service != null) {
      final Map<BiConsumer<Address, byte[]>, Executor> listeners = service.listeners.get(subject);
      if (listeners != null) {
        listeners.forEach(
            (listener, executor) -> executor.execute(() -> listener.accept(this.address, message)));
      }
    }
  }

  @Override
  public synchronized void addListener(
      final String subject, final BiConsumer<Address, byte[]> listener, final Executor executor) {
    listeners.computeIfAbsent(subject, s -> Maps.newConcurrentMap()).put(listener, executor);
  }

  @Override
  public synchronized void removeListener(
      final String subject, final BiConsumer<Address, byte[]> listener) {
    final Map<BiConsumer<Address, byte[]>, Executor> listeners = this.listeners.get(subject);
    if (listeners != null) {
      listeners.remove(listener);
      if (listeners.isEmpty()) {
        this.listeners.remove(subject);
      }
    }
  }

  @Override
  public CompletableFuture<UnicastService> start() {
    services.put(address, this);
    started.set(true);
    return CompletableFuture.completedFuture(this);
  }

  @Override
  public boolean isRunning() {
    return started.get();
  }

  @Override
  public CompletableFuture<Void> stop() {
    services.remove(address);
    started.set(false);
    return CompletableFuture.completedFuture(null);
  }
}
