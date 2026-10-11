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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.rest.service;

import com.anyilanxin.kunpeng.gateway.rest.service.vo.ClusterTopologyVo;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.GatewayDiscoveryVo;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.GatewayLoadVo;
import reactor.core.publisher.Mono;

/**
 * 集群管理查询业务（本地拓扑快照与网关发现，无 broker 往返）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ClusterService {

  /** 集群拓扑快照：成员、分区角色与健康。 */
  Mono<ClusterTopologyVo> topology();

  /** 集群网关发现：允许客户端发现的网关接入地址列表。 */
  Mono<GatewayDiscoveryVo> gateways();

  /** 指定网关的负载（活跃连接数；非本机网关经集群通信转发查询）。 */
  Mono<GatewayLoadVo> gatewayLoad(String grpcAddress);
}
