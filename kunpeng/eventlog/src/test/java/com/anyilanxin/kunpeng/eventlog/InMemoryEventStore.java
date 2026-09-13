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
package com.anyilanxin.kunpeng.eventlog;

import com.anyilanxin.kunpeng.eventlog.storage.EventStore;
import com.anyilanxin.kunpeng.eventlog.storage.EventStoreReader;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.MutableDirectBuffer;

/** 测试用内存存储：同步（默认）或单线程异步提交；可注入同步失败；记录追加顺序 */
public final class InMemoryEventStore implements EventStore {

  private final List<UnsafeBuffer> blocks = new CopyOnWriteArrayList<>();
  private final List<long[]> ranges = new CopyOnWriteArrayList<>();
  private final List<Long> appendOrder = new CopyOnWriteArrayList<>(); // firstPosition 序列
  private final List<CommitListener> commitListeners = new CopyOnWriteArrayList<>();
  private final AtomicInteger failuresToInject = new AtomicInteger();

  private ExecutorService asyncCommitExecutor;
  private volatile boolean closed;

  /** 开启异步提交模式（单线程, 模拟 raft 提交线程） */
  public InMemoryEventStore withAsyncCommit() {
    this.asyncCommitExecutor = Executors.newSingleThreadExecutor(r -> {
      final Thread thread = new Thread(r, "inmemory-commit");
      thread.setDaemon(true);
      return thread;
    });
    return this;
  }

  /** 接下来 N 次追加抛同步异常 */
  public void failNextAppends(final int n) {
    failuresToInject.set(n);
  }

  public List<Long> appendOrder() {
    return appendOrder;
  }

  public int blockCount() {
    return blocks.size();
  }

  public int firstBlockSize() {
    return blocks.isEmpty() ? 0 : blocks.get(0).capacity();
  }

  @Override
  public EventStoreReader newReader() {
    return new InMemoryReader();
  }

  @Override
  public void append(
      final long firstPosition, final long lastPosition, final BufferWriter block,
      final AppendListener listener) {
    if (closed) {
      throw new IllegalStateException("store closed");
    }
    if (failuresToInject.getAndUpdate(v -> v > 0 ? v - 1 : 0) > 0) {
      throw new RuntimeException("注入的追加失败: " + firstPosition);
    }
    final ExpandableArrayBuffer copy = new ExpandableArrayBuffer(block.getLength());
    block.write(copy, 0);
    blocks.add(new UnsafeBuffer(copy, 0, block.getLength()));
    ranges.add(new long[] {firstPosition, lastPosition});
    appendOrder.add(firstPosition);

    listener.onWrite(firstPosition, lastPosition);
    if (asyncCommitExecutor != null) {
      asyncCommitExecutor.execute(() -> commit(firstPosition, lastPosition, listener));
    } else {
      commit(firstPosition, lastPosition, listener);
    }
  }

  private void commit(final long first, final long last, final AppendListener listener) {
    listener.onCommit(first, last);
    for (final CommitListener commitListener : commitListeners) {
      commitListener.onCommit();
    }
  }

  @Override
  public void addCommitListener(final CommitListener listener) {
    commitListeners.add(listener);
  }

  @Override
  public void removeCommitListener(final CommitListener listener) {
    commitListeners.remove(listener);
  }

  public void shutdown() {
    closed = true;
    if (asyncCommitExecutor != null) {
      asyncCommitExecutor.shutdownNow();
    }
  }

  /** 追加顺序记录的并发安全访问 */
  public synchronized List<Long> appendOrderSnapshot() {
    return new ArrayList<>(appendOrder);
  }

  private final class InMemoryReader implements EventStoreReader {

    private int index;

    @Override
    public boolean hasNext() {
      return index < blocks.size();
    }

    @Override
    public DirectBuffer next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return blocks.get(index++);
    }

    @Override
    public void seek(final long position) {
      // 最后一个 first <= position 的块；越界钳到首/末块
      if (blocks.isEmpty() || position <= ranges.get(0)[0]) {
        index = 0;
        return;
      }
      index = blocks.size() - 1;
      for (int i = 0; i < ranges.size(); i++) {
        if (ranges.get(i)[0] > position) {
          index = i - 1;
          return;
        }
      }
    }

    @Override
    public void close() {
      // 无资源
    }
  }
}
