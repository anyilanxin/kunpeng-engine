/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.configuration.gateway;

import com.anyilanxin.kunpeng.configuration.SecurityCfg;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/** gateway 配置，定义请求超时、客户端发现开关及网络、线程、长轮询等子配置。 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class GatewayCfg {

  private Duration requestTimeout = Duration.ofSeconds(15);

  /** 该网关是否允许被客户端发现。 */
  private boolean allowClientDiscovery = true;

  @NestedConfigurationProperty private GatewayGrpcNetworkCfg network = new GatewayGrpcNetworkCfg();

  @NestedConfigurationProperty private GatewayThreadsCfg threads = new GatewayThreadsCfg();

  @NestedConfigurationProperty private LongPollingCfg longPolling = new LongPollingCfg();

  public void init(final String basePath) {
    if (network.getSecurity() == null) {
      network.setSecurity(new SecurityCfg());
    }
    network.init(basePath);
  }
}
