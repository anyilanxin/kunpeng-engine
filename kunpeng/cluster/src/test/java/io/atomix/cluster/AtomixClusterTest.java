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
package io.atomix.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.utils.net.Address;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

/** Atomix cluster test. */
public class AtomixClusterTest {
  private static final int TIMEOUT_IN_S = 90;

  @Rule public final AtomixClusterRule atomixClusterRule = new AtomixClusterRule();

  @Test
  public void testStopStartConsensus() throws Exception {
    // given
    final var atomix =
        atomixClusterRule
            .startAtomix(1, Arrays.asList(1), AtomixClusterBuilder::build)
            .get(TIMEOUT_IN_S, TimeUnit.SECONDS);

    // when
    final var stopFuture = atomix.stop();

    // then
    assertThat(stopFuture).succeedsWithin(TIMEOUT_IN_S, TimeUnit.SECONDS);
    assertThat(stopFuture).isDone();
  }

  @Test
  public void shouldFailStartAfterStop() throws Exception {
    // given
    final var atomix =
        atomixClusterRule
            .startAtomix(1, Arrays.asList(1), AtomixClusterBuilder::build)
            .get(TIMEOUT_IN_S, TimeUnit.SECONDS);
    atomix.stop().get(TIMEOUT_IN_S, TimeUnit.SECONDS);

    // when
    try {
      atomix.start().get(TIMEOUT_IN_S, TimeUnit.SECONDS);
      Assertions.fail("Expected ExecutionException");
    } catch (final ExecutionException ex) {
      // then
      assertThat(ex.getCause() instanceof IllegalStateException).isTrue();
      assertThat(ex.getCause().getMessage()).isEqualTo("Cluster instance is shutdown");
    }
  }

  @Test
  public void testBootstrap() throws Exception {
    final Collection<Node> bootstrapLocations =
        Arrays.asList(
            Node.builder().withId("foo").withAddress(Address.from("localhost:5000")).build(),
            Node.builder().withId("bar").withAddress(Address.from("localhost:5001")).build(),
            Node.builder().withId("baz").withAddress(Address.from("localhost:5002")).build());

    final AtomixCluster cluster1 =
        AtomixCluster.builder(atomixClusterRule.registry)
            .withMemberId("foo")
            .withHost("localhost")
            .withPort(5000)
            .withMembershipProvider(
                BootstrapDiscoveryProvider.builder().withNodes(bootstrapLocations).build())
            .build();
    cluster1.start().join();

    assertThat(cluster1.getMembershipService().getLocalMember().id().id()).isEqualTo("foo");

    final AtomixCluster cluster2 =
        AtomixCluster.builder(atomixClusterRule.registry)
            .withMemberId("bar")
            .withHost("localhost")
            .withPort(5001)
            .withMembershipProvider(
                BootstrapDiscoveryProvider.builder().withNodes(bootstrapLocations).build())
            .build();
    cluster2.start().join();

    assertThat(cluster2.getMembershipService().getLocalMember().id().id()).isEqualTo("bar");

    final AtomixCluster cluster3 =
        AtomixCluster.builder(atomixClusterRule.registry)
            .withMemberId("baz")
            .withHost("localhost")
            .withPort(5002)
            .withMembershipProvider(
                BootstrapDiscoveryProvider.builder().withNodes(bootstrapLocations).build())
            .build();
    cluster3.start().join();

    assertThat(cluster3.getMembershipService().getLocalMember().id().id()).isEqualTo("baz");

    final List<CompletableFuture<Void>> futures =
        Stream.of(cluster1, cluster2, cluster3)
            .map(AtomixCluster::stop)
            .collect(Collectors.toList());
    try {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
    } catch (final Exception e) {
      // Do nothing
    }
  }
}
