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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.logstorage;

import com.anyilanxin.kunpeng.cluster.raft.logentry.LogAppender;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.IndexedRaftLogEntry;
import com.anyilanxin.kunpeng.eventlog.storage.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Raft 追加生命周期 → EventStore.AppendListener（onWriteError/onCommitError 统一 onFailure） */
public final class AdminRaftAppendListenerAdapter implements LogAppender.AppendListener {

  private static final Logger LOG = LoggerFactory.getLogger(AdminRaftAppendListenerAdapter.class);

  private final long lastPosition;
  private final EventStore.AppendListener delegate;

  public AdminRaftAppendListenerAdapter(
      final long lastPosition, final EventStore.AppendListener delegate) {
    this.lastPosition = lastPosition;
    this.delegate = delegate;
  }

  @Override
  public void onWrite(final IndexedRaftLogEntry indexed) {
    delegate.onWrite(
        indexed.index(),
        indexed.isApplicationEntry() ? indexed.getApplicationEntry().highestPosition() : -1);
  }

  @Override
  public void onWriteError(final Throwable error) {
    LOG.debug(
        "Raft append failed at lastPosition={}, forwarding to FlowControl", lastPosition, error);
    delegate.onFailure(lastPosition, error);
  }

  @Override
  public void onCommit(final long index, final long highestPosition) {
    delegate.onCommit(index, highestPosition);
  }

  @Override
  public void onCommitError(final long index, final Throwable error) {
    LOG.debug(
        "Raft commit failed at index={}, lastPosition={}, forwarding to FlowControl",
        index,
        lastPosition,
        error);
    delegate.onFailure(lastPosition, error);
  }
}
