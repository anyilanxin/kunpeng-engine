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
package com.anyilanxin.kunpeng.configuration.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.configuration.SecurityCfg;
import com.anyilanxin.kunpeng.configuration.cluster.ClusterCfg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link GatewayGrpcNetworkCfg} 绑定缺省（通配）与通告地址解析（显式 grpc-host &gt; 具体绑定地址 &gt; 自动发现网卡）测试
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class GatewayGrpcNetworkCfgTest {

  private ClusterCfg clusterCfg;

  @BeforeEach
  void setUp() {
    clusterCfg = new ClusterCfg();
    clusterCfg.init("build/gateway-network-test");
  }

  /** 裸构造的 security 为 null, init 会走 security.init —— 测试按装配层形态补齐 */
  private GatewayGrpcNetworkCfg newNetworkCfg() {
    final GatewayGrpcNetworkCfg cfg = new GatewayGrpcNetworkCfg();
    cfg.setSecurity(new SecurityCfg());
    return cfg;
  }

  @Test
  void shouldBindWildcardAndAdvertiseDiscoveredHostByDefault() {
    final GatewayGrpcNetworkCfg cfg = newNetworkCfg();
    cfg.init(clusterCfg, "build/gateway-network-test");

    // 绑定缺省通配: 回环与所有网卡均可接入, 客户端连 127.0.0.1 不再被具体网卡绑定拒绝
    assertThat(cfg.getHost()).isEqualTo("0.0.0.0");
    // 通告不得是通配地址: 回落自动发现的网卡地址（无网卡环境为回环）, 保证客户端可接入
    assertThat(cfg.advertisedGrpcHost()).isNotBlank().isNotEqualTo("0.0.0.0").isNotEqualTo("::");
  }

  @Test
  void shouldAdvertiseConfiguredConcreteBindHost() {
    final GatewayGrpcNetworkCfg cfg = newNetworkCfg();
    cfg.setHost("192.168.50.111");
    cfg.init(clusterCfg, "build/gateway-network-test");

    assertThat(cfg.advertisedGrpcHost()).isEqualTo("192.168.50.111");
  }

  @Test
  void shouldAdvertiseExplicitGrpcHostOverWildcardBind() {
    final GatewayGrpcNetworkCfg cfg = newNetworkCfg();
    cfg.setGrpcHost("gateway.example.com");
    cfg.init(clusterCfg, "build/gateway-network-test");

    assertThat(cfg.getHost()).isEqualTo("0.0.0.0");
    assertThat(cfg.advertisedGrpcHost()).isEqualTo("gateway.example.com");
  }
}
