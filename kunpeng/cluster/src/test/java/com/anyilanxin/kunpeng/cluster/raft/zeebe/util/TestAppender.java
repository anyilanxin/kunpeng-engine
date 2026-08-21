/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.cluster.raft.zeebe.util;

import com.anyilanxin.kunpeng.cluster.raft.storage.log.IndexedRaftLogEntry;
import com.anyilanxin.kunpeng.cluster.raft.zeebe.ZeebeLogAppender;
import com.anyilanxin.kunpeng.cluster.raft.zeebe.ZeebeLogAppender.AppendListener;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestAppender implements AppendListener {
  private final BlockingQueue<IndexedRaftLogEntry> written;
  private final BlockingQueue<Long> committed;
  private final BlockingQueue<Throwable> errors;

  public TestAppender() {
    written = new LinkedBlockingQueue<>();
    committed = new LinkedBlockingQueue<>();
    errors = new LinkedBlockingQueue<>();
  }

  @Override
  public void onWrite(final IndexedRaftLogEntry indexed) {
    written.offer(indexed);
  }

  @Override
  public void onWriteError(final Throwable error) {
    errors.offer(error);
  }

  @Override
  public void onCommit(final long index, final long highestPosition) {
    committed.offer(index);
  }

  @Override
  public void onCommitError(final long index, final Throwable error) {
    errors.offer(error);
  }

  public IndexedRaftLogEntry append(
      final ZeebeLogAppender appender,
      final long lowest,
      final long highest,
      final ByteBuffer data) {
    appender.appendEntry(lowest, highest, data, this);
    return pollWritten();
  }

  public IndexedRaftLogEntry pollWritten() {
    return takeUnchecked(written);
  }

  public Long pollCommitted() {
    return takeUnchecked(committed);
  }

  public Throwable pollError() {
    return takeUnchecked(errors);
  }

  public List<IndexedRaftLogEntry> getWritten() {
    return new ArrayList<>(written);
  }

  public List<Long> getCommitted() {
    return new ArrayList<>(committed);
  }

  public List<Throwable> getErrors() {
    return new ArrayList<>(errors);
  }

  private <T> T takeUnchecked(final BlockingQueue<T> queue) {
    try {
      return queue.take();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
