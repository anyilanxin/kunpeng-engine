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

import static com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.NettyDnsMetricsDoc.ERROR;
import static com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.NettyDnsMetricsDoc.FAILED;
import static com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.NettyDnsMetricsDoc.SUCCESS;
import static com.anyilanxin.kunpeng.cluster.cluster.messaging.impl.NettyDnsMetricsDoc.WRITTEN;

import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.resolver.dns.DnsQueryLifecycleObserver;
import java.net.InetSocketAddress;
import java.util.List;
import net.jcip.annotations.ThreadSafe;

/** Netty DNS 解析相关指标采集 */
@ThreadSafe
final class NettyDnsMetrics implements DnsQueryLifecycleObserver {

  private final MeterRegistry registry;

  NettyDnsMetrics(final MeterRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void queryWritten(final InetSocketAddress dnsServerAddress, final ChannelFuture future) {
    Micrometers.counter(WRITTEN, registry).increment();
  }

  @Override
  public void queryCancelled(final int queriesRemaining) {}

  @Override
  public DnsQueryLifecycleObserver queryRedirected(final List<InetSocketAddress> nameServers) {
    return this;
  }

  @Override
  public DnsQueryLifecycleObserver queryCNAMEd(final DnsQuestion cnameQuestion) {
    return this;
  }

  @Override
  public DnsQueryLifecycleObserver queryNoAnswer(final DnsResponseCode code) {
    Micrometers.counter(FAILED, registry, "code", code.toString()).increment();
    return this;
  }

  @Override
  public void queryFailed(final Throwable cause) {
    Micrometers.counter(ERROR, registry).increment();
  }

  @Override
  public void querySucceed() {
    Micrometers.counter(SUCCESS, registry).increment();
  }
}
