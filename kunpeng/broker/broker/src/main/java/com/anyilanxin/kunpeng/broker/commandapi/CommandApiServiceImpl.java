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
package com.anyilanxin.kunpeng.broker.commandapi;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.CommandApiHandle;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.CommandApiService;
import com.anyilanxin.kunpeng.broker.monitoring.DiskSpaceUsageListener;
import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.concurrent.IdGenerator;

/**
 * 业务面命令 API 服务：actor 化的命令接收与处理器分发。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class CommandApiServiceImpl extends Actor
    implements CommandApiService, DiskSpaceUsageListener {
  private final Int2ObjectHashMap<EventLogWriter> leadingStreams = new Int2ObjectHashMap<>();
  private final CommandApiHandleImpl handle;

  public CommandApiServiceImpl(
      final MessagingService messagingService, final IdGenerator idGenerator) {
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
  public void onDiskSpaceNotAvailable() {
    actor.run(handle::onDiskSpaceNotAvailable);
  }

  @Override
  public void onDiskSpaceAvailable() {
    actor.run(handle::onDiskSpaceAvailable);
  }

  @Override
  public CommandApiHandle gettCommandApiHandle() {
    return handle;
  }

  @Override
  public ActorFuture<Void> registerHandlers(
      final RaftPartitionSource partitionSource, final EventLog logStream) {
    final var future = actor.<Void>createFuture();
    actor.run(
        () -> {
          final var logStreamWriter = logStream.newWriter();
          handle.registerHandlers(partitionSource, logStreamWriter);
          future.complete(null);
        });
    return future;
  }

  @Override
  public ActorFuture<Void> unregisterHandlers(final RaftPartitionSource partitionSource) {
    final var future = actor.<Void>createFuture();
    actor.run(
        () -> {
          handle.unregisterHandlers(partitionSource);
          future.complete(null);
        });
    return future;
  }
}
