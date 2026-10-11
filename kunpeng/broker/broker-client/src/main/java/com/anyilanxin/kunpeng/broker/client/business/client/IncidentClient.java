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
package com.anyilanxin.kunpeng.broker.client.business.client;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.incident.request.IncidentResolveRequest;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponse;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.incident.resolve.IncidentResolveResponseRecord;
import java.util.concurrent.CompletableFuture;

/**
 * 事件（incident）域命令客户端。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface IncidentClient {
  /** 解决事件 */
  CompletableFuture<BrokerResponse<IncidentResolveResponseRecord>> resolveIncident(
      IncidentResolveRequest request);
}
