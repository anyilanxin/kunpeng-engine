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
package com.anyilanxin.kunpeng.configuration.cluster;

import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.configuration.SecurityCfg;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 集群网络信息配置，定义起始端口、端口偏移、优先地址及各类消息配置。
 *
 * @author zxuanhong
 * @since
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@ToString
public class NetworkInfoCfg {
  private static final int DEFAULT_START_PORT = 2026;
  private static final int DEFAULT_PORT_OFFSET = 1;
  private static final String DEFAULT_ADVERTISED_HOST =
      Address.defaultAdvertisedHost().getHostAddress();
  private static final String DEFAULT_HOST = "0.0.0.0";

  /** 起始端口 */
  private int startPort = DEFAULT_START_PORT;

  /** 端口偏移量 */
  private int portOffset = DEFAULT_PORT_OFFSET;

  /** 优先地址 */
  private String advanceHost;

  /** 加密信息 */
  @NestedConfigurationProperty private SecurityCfg security = new SecurityCfg();

  /** 集群消息配置 */
  @NestedConfigurationProperty private MembershipNetworkCfg membership = new MembershipNetworkCfg();

  /** 客户端消息配置 */
  @NestedConfigurationProperty private ClientNetworkCfg client = new ClientNetworkCfg();

  /** 业务端消息配置 */
  @NestedConfigurationProperty private RaftNetworkCfg business = new RaftNetworkCfg();

  public void init(final String basePath) {
    if (StringUtils.isBlank(advanceHost)) {
      advanceHost = DEFAULT_ADVERTISED_HOST;
    }
    if (StringUtils.isBlank(advanceHost)) {
      advanceHost = DEFAULT_HOST;
    }
    if (portOffset <= 0) {
      throw new IllegalArgumentException("portOffset must be greater than 0");
    }
    security.init(basePath);
    if (startPort <= 0 || (startPort + portOffset * 2) >= 65535) {
      throw new IllegalArgumentException("(startPort + portOffset * 2) must be less than 65535");
    }
    membership.init(this, basePath);
    client.init(this, basePath);
    business.init(this, basePath);
  }
}
