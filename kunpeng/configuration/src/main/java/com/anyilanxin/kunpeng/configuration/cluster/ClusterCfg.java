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

import com.anyilanxin.kunpeng.configuration.ZoneType;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 集群配置，定义节点 ID、节点类型、集群名、种子节点与成员/网络配置。
 *
 * @author zxuanhong
 * @since
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ClusterCfg {

  public static final String DEFAULT_CLUSTER_NAME = "kunpeng-cluster";

  /** 节点 id */
  private String nodeId = "node-1";

  /** 节点类型，默认 broker */
  private ZoneType nodeType = ZoneType.BROKER;

  private String clusterName = DEFAULT_CLUSTER_NAME;

  /** 集群种子节点信息 */
  private List<String> seedNodes = new ArrayList<>();

  /** 成员配置 */
  private MembershipCfg membership = new MembershipCfg();

  /** 网络配置信息 */
  @NestedConfigurationProperty private NetworkInfoCfg network = new NetworkInfoCfg();

  public void init(final String base) {
    network.init(base);
  }
}
