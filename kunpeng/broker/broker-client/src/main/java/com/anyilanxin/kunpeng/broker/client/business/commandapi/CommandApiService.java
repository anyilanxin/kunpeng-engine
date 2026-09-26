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

import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * Supports sending arbitrary commands to another partition. Sending may be unreliable and fail
 * silently, it is up to the caller to detect this and retry.
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface CommandApiService {

  void onRecovered(final int partitionId);

  void onPaused(final int partitionId);

  void onRecoveredResource(final int resourceId);

  void onPausedResource(final int resourceId);

  ActorFuture<Void> registerHandlers(
      final RaftPartitionSource partitionSource, final EventLog logStream);

  ActorFuture<Void> unregisterHandlers(final RaftPartitionSource partitionSource);

  CommandApiHandle gettCommandApiHandle();
}
