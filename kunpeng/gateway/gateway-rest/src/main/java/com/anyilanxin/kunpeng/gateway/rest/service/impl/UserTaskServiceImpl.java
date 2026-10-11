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
package com.anyilanxin.kunpeng.gateway.rest.service.impl;

import static com.anyilanxin.kunpeng.gateway.grpc.utils.RequestUtil.ensureJsonSet;
import static com.anyilanxin.kunpeng.gateway.rest.core.BrokerCall.awaitAck;

import com.anyilanxin.kunpeng.broker.client.business.client.BusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.business.client.UserTaskClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request.UserTaskCancelRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request.UserTaskCompleteRequest;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.UserTaskCompleteDto;
import com.anyilanxin.kunpeng.gateway.rest.service.UserTaskService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 用户任务业务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Service
public class UserTaskServiceImpl implements UserTaskService {
  private final UserTaskClient userTask;

  public UserTaskServiceImpl(final BusinessCommandClient clients) {
    this.userTask = clients.userTask();
  }

  @Override
  public Mono<Void> complete(final long taskKey, final UserTaskCompleteDto dto) {
    final UserTaskCompleteRequest request =
        new UserTaskCompleteRequest(taskKey).setVariable(ensureJsonSet(dto.variable()));
    return awaitAck(userTask.completeUserTask(request));
  }

  @Override
  public Mono<Void> cancel(final long taskKey) {
    return awaitAck(userTask.cancelUserTask(new UserTaskCancelRequest(taskKey)));
  }
}
