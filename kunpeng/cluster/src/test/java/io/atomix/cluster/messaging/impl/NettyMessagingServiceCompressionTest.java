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

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.MessagingConfig.CompressionAlgorithm;
import io.atomix.utils.net.Address;
import io.atomix.test.util.SocketUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class NettyMessagingServiceCompressionTest {

  @AutoClose private final MeterRegistry registry = new SimpleMeterRegistry();

  @ParameterizedTest
  @EnumSource(CompressionAlgorithm.class)
  void shouldSendAndReceiveMessagesWhenCompressionEnabled(final CompressionAlgorithm algorithm) {
    // given
    var nextAddress = SocketUtil.getNextAddress();
    final var senderAddress = Address.from(nextAddress.getHostName(), nextAddress.getPort());
    final var config =
        new MessagingConfig()
            .setShutdownQuietPeriod(Duration.ofMillis(50))
            .setCompressionAlgorithm(algorithm);

    final var senderNetty =
        (ManagedMessagingService)
            new NettyMessagingService("test", senderAddress, config, registry).start().join();

    nextAddress = SocketUtil.getNextAddress();
    final var receiverAddress = Address.from(nextAddress.getHostName(), nextAddress.getPort());
    final var receiverNetty =
        (ManagedMessagingService)
            new NettyMessagingService("test", receiverAddress, config, registry).start().join();

    final String subject = "subject";
    final String requestString = "message";
    final String responseString = "success";
    receiverNetty.registerHandler(
        subject,
        (m, payload) -> {
          final String message = new String(payload);
          assertThat(message).isEqualTo(requestString);
          return CompletableFuture.completedFuture(responseString.getBytes());
        });

    // when
    final CompletableFuture<byte[]> response =
        senderNetty.sendAndReceive(receiverAddress, subject, requestString.getBytes());

    // then
    final var result = response.join();
    assertThat(new String(result)).isEqualTo(responseString);

    // teardown
    senderNetty.stop();
    receiverNetty.stop();
  }
}
