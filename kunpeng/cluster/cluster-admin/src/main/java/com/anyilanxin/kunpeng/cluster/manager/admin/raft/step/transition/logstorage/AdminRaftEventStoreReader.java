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

import com.anyilanxin.kunpeng.cluster.raft.storage.log.IndexedRaftLogEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLogReader;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.SerializedApplicationEntry;
import com.anyilanxin.kunpeng.eventlog.storage.EventStoreReader;
import java.util.NoSuchElementException;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/** Raft 日志读桥（look-ahead 游标）：按 ASQN seek，块 = journal 条目的应用数据（批帧） */
public final class AdminRaftEventStoreReader implements EventStoreReader {

  private final RaftLogReader reader;
  private final DirectBuffer currentBlockBuffer;
  private final DirectBuffer nextBlockBuffer;

  public AdminRaftEventStoreReader(final RaftLogReader reader) {
    this.reader = reader;
    currentBlockBuffer = new UnsafeBuffer();
    nextBlockBuffer = new UnsafeBuffer();
    reset();
  }

  @Override
  public void seek(final long position) {
    // 钳到 0：始终定位到日志上首个有效 ASQN（越界钳到首/末由 raft reader 语义覆盖）
    reader.seekToAsqn(Math.max(0, position));
    reset();
    readNextBlock();
  }

  @Override
  public void close() {
    reset();
    reader.close();
  }

  @Override
  public boolean hasNext() {
    return hasNextBlock() || readNextBlock();
  }

  @Override
  public DirectBuffer next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    currentBlockBuffer.wrap(nextBlockBuffer);
    nextBlockBuffer.wrap(0, 0);
    return currentBlockBuffer;
  }

  private boolean hasNextBlock() {
    return nextBlockBuffer.addressOffset() != 0;
  }

  private boolean readNextBlock() {
    while (reader.hasNext()) {
      final IndexedRaftLogEntry entry;
      try {
        entry = reader.next();
      } catch (final NoSuchElementException e) {
        // hasNext 与 next 之间日志被重置（角色切换窗口）
        return false;
      }
      if (entry.isApplicationEntry()) {
        final SerializedApplicationEntry nextEntry =
            (SerializedApplicationEntry) entry.getApplicationEntry();
        nextBlockBuffer.wrap(nextEntry.data());
        return true;
      }
    }
    return false;
  }

  private void reset() {
    currentBlockBuffer.wrap(0, 0);
    nextBlockBuffer.wrap(0, 0);
  }
}
