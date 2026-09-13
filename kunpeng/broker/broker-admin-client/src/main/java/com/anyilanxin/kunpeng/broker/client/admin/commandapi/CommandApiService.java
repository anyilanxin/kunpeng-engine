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
package com.anyilanxin.kunpeng.broker.client.admin.commandapi;

import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/** 支持向其他分区发送任意命令。发送过程可能不可靠并静默失败，需要由调用方自行检测失败并进行重试。 */
public interface CommandApiService {

  void onRecovered(final int partitionId);

  void onPaused(final int partitionId);

  void onRecoveredResource(final int resourceId);

  void onPausedResource(final int resourceId);

  ActorFuture<Void> registerHandlers(final EventLog logStream);

  ActorFuture<Void> unregisterHandlers();

  CommandApiHandle gettCommandApiHandle();
}
