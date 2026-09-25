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
package com.anyilanxin.kunpeng.broker.client.business.commandapi;

import com.anyilanxin.kunpeng.broker.client.business.BrokerResponseWriter;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.CommandApiValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;

/**
 * 命令 API 处理句柄：命令处理器的注册标识与生命周期管理。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface CommandApiHandle {

  <Response extends UnifiedRecordValue> void sendResponse(
      final BrokerResponseWriter<Response> response);

  <Response extends UnifiedRecordValue> BrokerResponseWriter<Response> newResponse(
      final CommandApiValueLifeCycle lifeCycle, final long requestId, final int partitionId);
}
