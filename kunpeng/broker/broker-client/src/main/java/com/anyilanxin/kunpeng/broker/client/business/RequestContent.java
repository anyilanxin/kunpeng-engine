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
package com.anyilanxin.kunpeng.broker.client.business;

import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.ResponseRecordValue;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 请求内容载体：请求与响应类型配对的组合 record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public record RequestContent<Response extends ResponseRecordValue>(
    RecordType requestType,
    String requestTypeResourceId,
    Address apiAddress,
    String topicName,
    boolean shouldRetry,
    Duration requestTimeout,
    byte[] data,
    CompletableFuture<BrokerResponse<Response>> responseFuture) {}
