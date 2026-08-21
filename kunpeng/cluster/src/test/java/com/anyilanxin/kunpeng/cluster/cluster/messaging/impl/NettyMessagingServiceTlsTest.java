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

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingException;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.cluster.utils.SocketUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;

final class NettyMessagingServiceTlsTest {
  @AutoClose private final MeterRegistry registry = new SimpleMeterRegistry();

  @Test
  void shouldAllowEnablingEndpointIdentification() throws CertificateException {
    // given
    final var serverAddress = SocketUtil.getNextAddress();
    final var clientAddress = SocketUtil.getNextAddress();
    final var certificate = new SelfSignedCertificate(serverAddress.getHostString());
    final var client = createSecureMessagingService(certificate, clientAddress, true);
    final var server = createSecureMessagingService(certificate, serverAddress);
    final var payload = "foo".getBytes();

    // when
    client.start().join();
    server.start().join();
    server.registerHandler(
        "topic",
        (sender, request) ->
            CompletableFuture.completedFuture((new String(request) + "bar").getBytes()));
    final var response = client.sendAndReceive(server.address(), "topic", payload).join();

    // then
    assertThat(response).isEqualTo("foobar".getBytes());
  }

  @Test
  void shouldCommunicateOverTls() throws CertificateException {
    // given
    final var serverAddress = SocketUtil.getNextAddress();
    final var clientAddress = SocketUtil.getNextAddress();
    final var certificate = new SelfSignedCertificate("invalid-hostname");
    final var client = createSecureMessagingService(certificate, clientAddress);
    final var server = createSecureMessagingService(certificate, serverAddress);
    final var payload = "foo".getBytes();

    // when
    client.start().join();
    server.start().join();
    server.registerHandler(
        "topic",
        (sender, request) ->
            CompletableFuture.completedFuture((new String(request) + "bar").getBytes()));
    final var response = client.sendAndReceive(server.address(), "topic", payload).join();

    // then
    assertThat(response).isEqualTo("foobar".getBytes());
  }

  @Test
  void shouldFailWhenClientIsNotUsingTls() throws CertificateException {
    // given
    final var serverAddress = SocketUtil.getNextAddress();
    final var certificate = new SelfSignedCertificate(serverAddress.getHostString());
    final var client = createInsecureMessagingService();
    final var server = createSecureMessagingService(certificate, serverAddress);
    final var payload = "foo".getBytes();

    // when
    client.start().join();
    server.start().join();
    server.registerHandler(
        "topic",
        (sender, request) ->
            CompletableFuture.completedFuture((new String(request) + "bar").getBytes()));
    final var response =
        client.sendAndReceive(server.address(), "topic", payload, true, Duration.ofSeconds(10));

    // then
    assertThat(response)
        .failsWithin(Duration.ofSeconds(10))
        .withThrowableOfType(ExecutionException.class)
        .havingRootCause()
        .isInstanceOf(MessagingException.ConnectionClosed.class);
  }

  @Test
  void shouldFailWhenServerIsNotUsingTls() throws CertificateException {
    // given
    final var serverAddress = SocketUtil.getNextAddress();
    final var clientAddress = SocketUtil.getNextAddress();
    final var certificate = new SelfSignedCertificate(serverAddress.getHostString());
    final var server = createInsecureMessagingService();
    final var client = createSecureMessagingService(certificate, clientAddress);
    final var payload = "foo".getBytes();

    // when
    client.start().join();
    server.start().join();
    server.registerHandler(
        "topic",
        (sender, request) ->
            CompletableFuture.completedFuture((new String(request) + "bar").getBytes()));
    final var response =
        client.sendAndReceive(server.address(), "topic", payload, true, Duration.ofSeconds(1));

    // then
    assertThat(response)
        .failsWithin(Duration.ofSeconds(2))
        .withThrowableOfType(ExecutionException.class)
        .havingRootCause()
        .isInstanceOf(MessagingException.ConnectionClosed.class);
  }

  private NettyMessagingService createInsecureMessagingService() {
    final var config =
        new MessagingConfig().setPort(SocketUtil.getNextAddress().getPort()).setTlsEnabled(false);
    return new NettyMessagingService(
        "cluster", Address.from(config.getPort()), config, registry);
  }

  private NettyMessagingService createSecureMessagingService(
      final SelfSignedCertificate certificate, final InetSocketAddress address) {
    return createSecureMessagingService(certificate, address, false);
  }

  private NettyMessagingService createSecureMessagingService(
      final SelfSignedCertificate certificate,
      final InetSocketAddress address,
      final boolean tlsEndpointIdentificationEnabled) {
    final var config =
        new MessagingConfig()
            .setPort(address.getPort())
            .setTlsEnabled(true)
            .setTlsEndpointIdentificationEnabled(tlsEndpointIdentificationEnabled)
            .setCertificateChain(certificate.certificate())
            .setPrivateKey(certificate.privateKey());
    return new NettyMessagingService(
        "cluster", Address.from(address.getHostString(), address.getPort()), config, registry);
  }
}
