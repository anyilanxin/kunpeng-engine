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
package io.atomix.cluster.messaging.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.atomix.cluster.messaging.ManagedUnicastService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.utils.net.Address;
import io.atomix.test.util.SocketUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.jodah.concurrentunit.ConcurrentTestCase;
import org.agrona.CloseHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AutoClose;

/** Netty unicast service test. */
public class NettyUnicastServiceTest extends ConcurrentTestCase {
  ManagedUnicastService service1;
  ManagedUnicastService service2;
  Address address1;
  Address address2;
  @AutoClose private final MeterRegistry registry = new SimpleMeterRegistry();

  @Test
  public void testUnicast() throws Exception {
    service1.addListener(
        "test",
        (address, payload) -> {
          assertThat(address).isEqualTo(address2);
          assertThat(payload).containsExactly("Hello world!".getBytes());
          resume();
        });

    service2.unicast(address1, "test", "Hello world!".getBytes());
    await(5000);
  }

  @Test
  public void shouldNotThrowExceptionWhenServiceStopped() {
    // given
    service2.stop();

    // when - then
    assertThatCode(() -> service2.unicast(address1, "test", "Hello world!".getBytes()))
        .doesNotThrowAnyException();
  }

  @Before
  public void setUp() throws Exception {
    address1 = Address.from("127.0.0.1", SocketUtil.getNextAddress().getPort());
    address2 = Address.from("127.0.0.1", SocketUtil.getNextAddress().getPort());

    final String clusterId = "testClusterId";
    service1 =
        new NettyUnicastService(clusterId, address1, new MessagingConfig(), "Unicast-1", registry);
    service1.start().join();

    service2 =
        new NettyUnicastService(clusterId, address2, new MessagingConfig(), "Unicast-2", registry);
    service2.start().join();
  }

  @After
  public void tearDown() throws Exception {
    CloseHelper.quietCloseAll(() -> service1.stop().join(), () -> service2.stop().join());
  }
}
