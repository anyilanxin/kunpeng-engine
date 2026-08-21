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
package com.anyilanxin.kunpeng.cluster.raft.storage.log;

import com.anyilanxin.kunpeng.utils.CloseableSilently;
import com.anyilanxin.kunpeng.cluster.raft.journal.CheckedJournalException.FlushException;
import com.anyilanxin.kunpeng.cluster.raft.journal.Journal;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.ThreadContextFactory;

/**
 * {@link com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLog} 的可插拔 fsync 策略。
 *
 * <p>各实现沿"持久性-吞吐"权衡轴分布：
 *
 * <ul>
 *   <li>{@link DirectFlusher}：每次提示都同步 fsync，最安全也最慢（缺省策略）；
 *   <li>{@link DelayedFlusher}：把 fsync 合并到可配置延迟之后批量执行，介于两个极端之间，
 *       建议在满足吞吐目标的前提下取最小延迟；
 *   <li>{@link NoopFlusher}：从不主动 fsync，持久性完全交给操作系统，最快但崩溃时可能丢数据。
 * </ul>
 */
@FunctionalInterface
public interface RaftLogFlusher extends CloseableSilently {

  /**
   * 提示日志中存在未落盘数据。
   *
   * <p>实现可自行选择立即 fsync 或延后处理。
   *
   * @param journal 待刷盘的日志
   */
  void flush(final Journal journal) throws FlushException;

  /** 为 true 表示 {@link #flush(Journal)} 完全同步，返回即意味着持久性已生效。 */
  default boolean isDirect() {
    return false;
  }

  /** 缺省为空实现：策略本身通常不持有需要显式释放的资源。 */
  @Override
  default void close() {}

  /**
   * {@link RaftLogFlusher} 实例的工厂。
   *
   * <p>{@link com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext} 的构造过程对调用方不透明，因此通过工厂函数
   * 间接注入刷盘策略。
   */
  @FunctionalInterface
  interface Factory {

    /**
     * 共享单例的持有者。
     *
     * <p>两个内置无状态策略是线程安全的，用初始化安全的枚举承载，避免暴露可变静态字段。
     */
    enum Shared {
      /** 直刷单例。 */
      DIRECT(new DirectFlusher()),

      /** 空刷单例。 */
      NOOP(new NoopFlusher());

      private final RaftLogFlusher flusher;

      Shared(final RaftLogFlusher flusher) {
        this.flusher = flusher;
      }

      RaftLogFlusher flusher() {
        return flusher;
      }
    }

    /** 线程安全、可复用的共享 {@link DirectFlusher} 实例。 */
    DirectFlusher DIRECT = (DirectFlusher) Shared.DIRECT.flusher();

    /** 线程安全、可复用的共享 {@link NoopFlusher} 实例。 */
    NoopFlusher NOOP = (NoopFlusher) Shared.NOOP.flusher();

    /**
     * 创建刷盘器。
     *
     * <p>若需要异步线程，应使用传入的工厂创建，并由刷盘器自行负责关闭。
     *
     * @param threadFactory 为异步工作提供线程上下文
     * @return 就绪可用的刷盘器
     */
    RaftLogFlusher createFlusher(final ThreadContextFactory threadFactory);

    /** 预置工厂方法：始终返回共享的直刷单例。 */
    static DirectFlusher direct(final ThreadContextFactory threadFactory) {
      return DIRECT;
    }

    /** 预置工厂方法：始终返回共享的空刷单例。 */
    static NoopFlusher noop(final ThreadContextFactory threadFactory) {
      return NOOP;
    }
  }

  /** 阻塞到已写入数据真正落盘后才返回的实现。 */
  final class DirectFlusher implements RaftLogFlusher {

    @Override
    public void flush(final Journal journal) throws FlushException {
      journal.flush();
    }

    @Override
    public boolean isDirect() {
      return true;
    }
  }

  /** 从不主动刷盘的实现，仅在快照前才会触发一次 fsync。 */
  final class NoopFlusher implements RaftLogFlusher {

    @Override
    public void flush(final Journal unusedJournal) {}
  }
}
