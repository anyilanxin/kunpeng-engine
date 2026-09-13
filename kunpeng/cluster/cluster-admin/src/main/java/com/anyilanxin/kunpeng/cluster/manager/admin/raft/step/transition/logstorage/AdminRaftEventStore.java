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

import com.anyilanxin.kunpeng.cluster.raft.RaftCommitListener;
import com.anyilanxin.kunpeng.cluster.raft.logentry.LogAppender;
import com.anyilanxin.kunpeng.eventlog.storage.EventStore;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Raft journal 桥（logstreams SPI 实现）：追加走 LogAppender 复制，提交回调转发通知 */
public class AdminRaftEventStore implements EventStore, RaftCommitListener {

  private final AdminAtomixReaderFactory readerFactory;
  private final LogAppender logAppender;
  private final Set<CommitListener> commitListeners = new CopyOnWriteArraySet<>();

  public AdminRaftEventStore(
      final AdminAtomixReaderFactory readerFactory, final LogAppender logAppender) {
    this.readerFactory = readerFactory;
    this.logAppender = logAppender;
  }

  public static AdminRaftEventStore ofPartition(
      final AdminAtomixReaderFactory readerFactory, final LogAppender appender) {
    return new AdminRaftEventStore(readerFactory, appender);
  }

  @Override
  public AdminRaftEventStoreReader newReader() {
    return new AdminRaftEventStoreReader(readerFactory.create());
  }

  @Override
  public void append(
      final long firstPosition,
      final long lastPosition,
      final BufferWriter block,
      final AppendListener listener) {
    logAppender.appendEntry(
        firstPosition,
        lastPosition,
        block,
        new AdminRaftAppendListenerAdapter(lastPosition, listener));
  }

  @Override
  public void addCommitListener(final CommitListener listener) {
    commitListeners.add(listener);
  }

  @Override
  public void removeCommitListener(final CommitListener listener) {
    commitListeners.remove(listener);
  }

  @Override
  public void onCommit(final long index) {
    commitListeners.forEach(CommitListener::onCommit);
  }
}
