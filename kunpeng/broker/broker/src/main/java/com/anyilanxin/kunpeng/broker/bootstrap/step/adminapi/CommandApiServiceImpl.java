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
package com.anyilanxin.kunpeng.broker.bootstrap.step.adminapi;

import com.anyilanxin.kunpeng.broker.bootstrap.step.idgenerator.NodeIdGeneratorService;
import com.anyilanxin.kunpeng.broker.client.admin.commandapi.CommandApiHandle;
import com.anyilanxin.kunpeng.broker.client.admin.commandapi.CommandApiService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * @author zxuanhong
 * @since
 */
public class CommandApiServiceImpl extends Actor implements CommandApiService {
  private final CommandApiHandleImpl handle;

  public CommandApiServiceImpl(
      final MessagingService messagingService, final NodeIdGeneratorService idGenerator) {
    handle = new CommandApiHandleImpl(messagingService, idGenerator, actor);
  }

  @Override
  public void onRecovered(final int partitionId) {}

  @Override
  public void onPaused(final int partitionId) {}

  @Override
  public void onRecoveredResource(final int resourceId) {}

  @Override
  public void onPausedResource(final int resourceId) {}

  @Override
  public CommandApiHandle gettCommandApiHandle() {
    return handle;
  }

  @Override
  public ActorFuture<Void> registerHandlers(final EventLog logStream) {
    final var future = actor.<Void>createFuture();
    actor.run(
        () -> {
          final var logStreamWriter = logStream.newWriter();
          handle.registerHandlers(logStreamWriter);
          future.complete(null);
        });
    return future;
  }

  @Override
  public ActorFuture<Void> unregisterHandlers() {
    final var future = actor.<Void>createFuture();
    actor.run(
        () -> {
          handle.unregisterHandlers();
          future.complete(null);
        });
    return future;
  }
}
