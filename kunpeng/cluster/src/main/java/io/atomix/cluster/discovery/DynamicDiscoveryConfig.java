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
package io.atomix.cluster.discovery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

/** 基于动态解析（DNS 等）的节点发现配置。 */
public class DynamicDiscoveryConfig extends NodeDiscoveryConfig {

  /** 刷新间隔的缺省值：每分钟重新解析一次地址。 */
  private static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofSeconds(60);

  /** 地址未显式携带端口时使用的缺省端口。 */
  private static final int DEFAULT_PORT = 26502;

  private int fallbackPort = DEFAULT_PORT;
  private Duration rescanPeriod = DEFAULT_REFRESH_INTERVAL;
  private Collection<String> endpoints = new ArrayList<>();

  @Override
  public NodeDiscoveryProvider.Type getType() {
    return DynamicDiscoveryProvider.TYPE;
  }

  /** 缺省端口：当配置地址没有写端口时按此端口生成节点地址。 */
  public int getDefaultPort() {
    return fallbackPort;
  }

  /** 设置缺省端口。 */
  public void setDefaultPort(final int defaultPort) {
    this.fallbackPort = defaultPort;
  }

  /** 需要解析的地址列表，元素形如 {@code "host:port"} 或 {@code "host"}。 */
  public Collection<String> getAddresses() {
    return endpoints;
  }

  /** 设置待解析地址列表。 */
  public DynamicDiscoveryConfig setAddresses(final Collection<String> addresses) {
    this.endpoints = addresses;
    return this;
  }

  /** 相邻两次地址重解析之间的间隔。 */
  public Duration getRefreshInterval() {
    return rescanPeriod;
  }

  /** 设置地址重解析间隔。 */
  public DynamicDiscoveryConfig setRefreshInterval(final Duration refreshInterval) {
    this.rescanPeriod = refreshInterval;
    return this;
  }
}
