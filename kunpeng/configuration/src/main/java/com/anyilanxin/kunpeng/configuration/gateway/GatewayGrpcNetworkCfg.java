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

import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.configuration.NetworkInfo;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.unit.DataSize;

/** gateway gRPC 网络配置，定义 gRPC 端口、流量控制窗口、keep-alive 等参数。 */
@Setter
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class GatewayGrpcNetworkCfg extends NetworkInfo {
  private static final int DEFAULT_PORT = 2024;
  private static final String DEFAULT_ADVERTISED_HOST =
      Address.defaultAdvertisedHost().getHostAddress();
  private static final String DEFAULT_HOST = "0.0.0.0";
  private Duration minKeepAliveInterval = Duration.ofSeconds(30);
  private DataSize maxMessageSize = DataSize.ofMegabytes(4);
  private String grpcHost;
  private int grpcPort = DEFAULT_PORT;
  private boolean customGrpcPort = false;
  private boolean customGrpcHost = false;

  /** 每条流的 HTTP/2 流量控制窗口。Netty 默认值（约 64KB）对高吞吐 RPC 载荷偏小； 8MB 能更好地摊薄大体积 BPMN 作业激活与流程实例写入的开销。 */
  private DataSize flowControlWindow = DataSize.ofMegabytes(8);

  /** HTTP/2 元数据（header）的最大大小。默认 8KB 对较丰富的鉴权/gRPC 追踪 header 偏小； 1MB 可提供充足的余量。 */
  private DataSize maxInboundMetadataSize = DataSize.ofMegabytes(1);

  /**
   * 服务端发起的 keep-alive ping 间隔。用于让服务端检测半开连接（例如位于 NAT 之后、被静默断开的客户端）。 与 {@link #keepAliveTimeout}
   * 配合使用。
   */
  private Duration keepAliveTime = Duration.ofMinutes(2);

  /** 判定连接死亡前等待 keep-alive ack 的时长。 */
  private Duration keepAliveTimeout = Duration.ofSeconds(20);

  public boolean userCustomGrpcPort() {
    return customGrpcPort;
  }

  public boolean userCustomGrpcHost() {
    return customGrpcHost;
  }

  public String getGrpcHost() {
    return grpcHost;
  }

  public void setGrpcHost(final String grpcHost) {
    this.grpcHost = grpcHost;
    customGrpcHost = true;
  }

  public int getGrpcPort() {
    return grpcPort;
  }

  public void setGrpcPort(final int grpcPort) {
    this.grpcPort = grpcPort;
    customGrpcPort = true;
  }

  @Override
  public void init(final String basePath) {
    if (getPort() <= 0) {
      setPort(DEFAULT_PORT);
    }
    if (StringUtils.isBlank(getHost())) {
      setHost(DEFAULT_ADVERTISED_HOST);
    }
    if (StringUtils.isBlank(getHost())) {
      setHost(DEFAULT_HOST);
    }
    super.init(basePath);
  }
}
