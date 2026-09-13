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

import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import com.anyilanxin.kunpeng.configuration.NetworkInfo;
import com.anyilanxin.kunpeng.configuration.gateway.ConfigurationDefaults;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;

/**
 * 成员通信网络配置，定义成员间通信的端口、心跳与消息压缩等参数。
 *
 * @author zxuanhong
 * @since
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class MembershipNetworkCfg extends NetworkInfo {
  @NestedConfigurationProperty
  private Duration requestTimeout = ConfigurationDefaults.DEFAULT_REQUEST_TIMEOUT;

  private MessagingConfig.CompressionAlgorithm messageCompression =
      MessagingConfig.CompressionAlgorithm.NONE;
  private DataSize socketSendBuffer = null;
  private DataSize socketReceiveBuffer = null;
  private Duration heartbeatTimeout = Duration.ofSeconds(15);
  private Duration heartbeatInterval = Duration.ofSeconds(5);

  public void init(final NetworkInfoCfg infoCfg, final String basePath) {
    if (getSecurity() == null) {
      setSecurity(infoCfg.getSecurity());
    }
    if (getHost() == null) {
      setHost(infoCfg.getAdvanceHost());
    }
    if (getPort() <= 0) {
      setPort(infoCfg.getStartPort());
    }
    super.init(basePath);
  }
}
