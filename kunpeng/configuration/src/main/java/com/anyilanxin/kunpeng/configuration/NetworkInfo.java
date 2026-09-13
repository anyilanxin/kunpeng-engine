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
package com.anyilanxin.kunpeng.configuration;

import java.net.InetSocketAddress;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 网络信息配置基类，包含端口、主机地址与安全配置。
 *
 * @author zxuanhong
 * @since
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class NetworkInfo {
  private int port;
  private String host;
  @NestedConfigurationProperty private SecurityCfg security;

  public InetSocketAddress getAdvertisedAddress() {
    return InetSocketAddress.createUnresolved(host, port);
  }

  public void init(final String basePath) {
    security.init(basePath);
  }
}
