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
package com.anyilanxin.kunpeng.gateway.rest.service.vo;

import com.anyilanxin.kunpeng.protocol.gateway.GatewayInfo;
import java.util.List;

/**
 * 集群网关发现结果。
 *
 * @param gateways 允许客户端发现的网关列表
 * @author zxuanhong
 * @since 2026.9.0
 */
public record GatewayDiscoveryVo(List<GatewayVo> gateways) {

  /** 单个网关接入信息。 */
  public record GatewayVo(
      String nodeId, String version, String grpcAddress, boolean allowClientDiscovery) {}

  public static GatewayVo of(final GatewayInfo info) {
    return new GatewayVo(
        info.getNodeId(), info.getVersion(), info.getGrpcAddress(), info.isAllowClientDiscovery());
  }
}
