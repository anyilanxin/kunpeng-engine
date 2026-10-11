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
package com.anyilanxin.kunpeng.broker.client.business.client.impl;

import com.anyilanxin.kunpeng.broker.client.business.client.UserTaskClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request.UserTaskCancelRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request.UserTaskCompleteRequest;
import com.anyilanxin.kunpeng.broker.client.core.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponse;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.cancel.UserTaskCancelResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.complete.UserTaskCompleteResponseRecord;
import java.util.concurrent.CompletableFuture;

/**
 * 用户任务域命令客户端实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DefaultUserTaskClient implements UserTaskClient {
  private final BrokerClient brokerClient;

  public DefaultUserTaskClient(final BrokerClient brokerClient) {
    this.brokerClient = brokerClient;
  }

  @Override
  public CompletableFuture<BrokerResponse<UserTaskCompleteResponseRecord>> completeUserTask(
      final UserTaskCompleteRequest request) {
    return brokerClient.sendRequest(request);
  }

  @Override
  public CompletableFuture<BrokerResponse<UserTaskCancelResponseRecord>> cancelUserTask(
      final UserTaskCancelRequest request) {
    return brokerClient.sendRequest(request);
  }
}
