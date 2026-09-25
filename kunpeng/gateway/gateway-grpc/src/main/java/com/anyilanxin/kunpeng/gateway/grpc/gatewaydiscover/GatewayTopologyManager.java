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
package com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover;

import com.anyilanxin.kunpeng.protocol.gateway.GatewayConnectorRecord;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayInfo;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 维护集群内的网关视图：查询在线网关列表，并按地址获取某个网关的负载情况。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface GatewayTopologyManager {
  List<GatewayInfo> getGateways();

  CompletableFuture<GatewayConnectorRecord> getGatewayLoad(String gatewayGrpcAddress);
}
