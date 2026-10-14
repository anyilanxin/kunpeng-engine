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
import com.anyilanxin.kunpeng.configuration.ZoneType;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 集群配置，定义节点 ID、节点类型、集群名、种子节点、节点落位（通告地址/端口阶梯/加密）与 membership 网络配置。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ClusterCfg {

  public static final String DEFAULT_CLUSTER_NAME = "kunpeng-cluster";

  private static final int DEFAULT_START_PORT = 24109;
  private static final int DEFAULT_PORT_OFFSET = 1;
  private static final String DEFAULT_ADVERTISED_HOST =
      Address.defaultAdvertisedHost().getHostAddress();
  private static final String DEFAULT_HOST = "0.0.0.0";

  /** 节点 id */
  private String nodeId = "node-1";

  /** 节点类型，默认 broker */
  private ZoneType nodeType = ZoneType.BROKER;

  private String clusterName = DEFAULT_CLUSTER_NAME;

  /** 集群种子节点信息 */
  private List<String> seedNodes = new ArrayList<>();

  /** 起始端口：membership 端口；broker 业务端口默认 startPort + portOffset */
  private int startPort = DEFAULT_START_PORT;

  /** 端口偏移量 */
  private int portOffset = DEFAULT_PORT_OFFSET;

  /** 优先地址（各网络平面默认通告地址） */
  private String advanceHost;

  /** 加密信息（各网络平面默认加密配置） */
  @NestedConfigurationProperty private SecurityCfg security = new SecurityCfg();

  /** 成员配置 */
  private MembershipCfg membership = new MembershipCfg();

  /** 集群（membership）网络配置 */
  @NestedConfigurationProperty private MembershipNetworkCfg network = new MembershipNetworkCfg();

  /**
   * 自寻址消息是否走 aeron:ipc 共享内存短路（目的地=本节点的消息不经 UDP 内核回环）。
   *
   * <p>单进程/单节点形态下收益最大：跨分区命令、job 推送回本机网关等自环流量从内核回环两次网络栈穿越 降为共享内存直拷（µs 级）。默认开启。
   */
  private boolean localIpcEnabled = true;

  public void init(final String base) {
    if (StringUtils.isBlank(advanceHost)) {
      advanceHost = DEFAULT_ADVERTISED_HOST;
    }
    if (StringUtils.isBlank(advanceHost)) {
      advanceHost = DEFAULT_HOST;
    }
    if (portOffset <= 0) {
      throw new IllegalArgumentException("portOffset must be greater than 0");
    }
    security.init(base);
    if (startPort <= 0 || (startPort + portOffset) >= 65535) {
      throw new IllegalArgumentException("(startPort + portOffset) must be less than 65535");
    }
    if ((startPort - portOffset) <= 0) {
      throw new IllegalArgumentException(
          "(startPort - portOffset) must be greater than 0: gateway gRPC port derives from it");
    }
    network.init(this, base);
  }
}
