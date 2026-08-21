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
package com.anyilanxin.kunpeng.cluster.raft.zeebe.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.RaftPartitionServer;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.IndexedRaftLogEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLogReader;
import com.anyilanxin.kunpeng.cluster.raft.zeebe.ZeebeLogAppender;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Utilities to help write tests; as these are utils, everything is public by default */
@SuppressWarnings("WeakerAccess")
public class ZeebeTestHelper {

  private static final long DEFAULT_TIMEOUT_MS = 10_000;
  private final Collection<ZeebeTestNode> nodes;

  public ZeebeTestHelper(final Collection<ZeebeTestNode> nodes) {
    this.nodes = nodes;
  }

  public ZeebeLogAppender awaitLeaderAppender(final int partitionId) {
    final RaftPartitionServer server = awaitLeaderServer(partitionId);
    return await(server::getAppender);
  }

  public RaftPartitionServer awaitLeaderServer(final int partitionId) {
    return awaitLeader(partitionId).getPartitionServer(partitionId);
  }

  public ZeebeTestNode awaitLeader(final int partitionId) {
    return await(() -> getLeader(partitionId));
  }

  public ZeebeTestNode awaitLeader(final int partitionId, final Collection<ZeebeTestNode> nodes) {
    return await(() -> getLeader(partitionId, nodes));
  }

  public Optional<ZeebeTestNode> getLeader(final int partitionId) {
    return getLeader(partitionId, nodes);
  }

  public Optional<ZeebeTestNode> getLeader(
      final int partitionId, final Collection<ZeebeTestNode> nodes) {
    return nodes.stream()
        .filter(n -> n.getPartition(partitionId).getRole() == Role.LEADER)
        .findFirst();
  }

  public void awaitAllContain(final int partitionId, final IndexedRaftLogEntry indexed) {
    awaitAllContains(nodes, partitionId, indexed);
  }

  public void awaitAllContains(
      final Collection<ZeebeTestNode> nodes,
      final int partitionId,
      final IndexedRaftLogEntry indexed) {
    await(() -> nodes.stream().allMatch(node -> containsIndexed(node, partitionId, indexed)));
  }

  public boolean containsIndexed(
      final ZeebeTestNode node, final int partitionId, final IndexedRaftLogEntry indexed) {
    final RaftPartitionServer partition = node.getPartitionServer(partitionId);
    return containsIndexed(partition, indexed);
  }

  public boolean containsIndexed(
      final RaftPartitionServer partition, final IndexedRaftLogEntry indexed) {
    try (final RaftLogReader reader = partition.openReader()) {
      reader.seek(indexed.index());

      if (reader.hasNext()) {
        final IndexedRaftLogEntry entry = reader.next();
        if (entry.index() == indexed.index()) {
          return isEntryEqualTo(entry, indexed);
        }
      }
    }

    return false;
  }

  public boolean isEntryEqualTo(
      final IndexedRaftLogEntry indexed, final IndexedRaftLogEntry other) {
    return indexed.equals(other);
  }

  public void await(final BooleanSupplier predicate) {
    final long tries = Duration.ofMillis(DEFAULT_TIMEOUT_MS).toNanos() / 100;
    boolean result = predicate.getAsBoolean();
    for (long i = 0; i < tries && !result; i++) {
      LockSupport.parkNanos(100);
      result = predicate.getAsBoolean();
    }

    assertThat(result).isTrue();
  }

  public void awaitContains(
      final ZeebeTestNode node, final int partitionId, final IndexedRaftLogEntry indexed) {
    await(() -> containsIndexed(node, partitionId, indexed));
  }

  public <T> T await(final Supplier<Optional<T>> supplier) {
    await(supplier, Optional::isPresent);
    final Optional<T> result = supplier.get();
    return result.get();
  }

  public <T> T await(final Supplier<T> supplier, final Predicate<T> condition) {
    await(() -> condition.test(supplier.get()));
    return supplier.get();
  }
}
