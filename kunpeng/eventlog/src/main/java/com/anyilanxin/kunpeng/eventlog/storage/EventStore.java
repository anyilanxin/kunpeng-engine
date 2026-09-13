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
package com.anyilanxin.kunpeng.eventlog.storage;

import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;

/**
 * 存储 SPI：追加块 + 提交通知 + 拉取读。生产实现由 broker 侧桥接 Raft journal （firstPosition/lastPosition 写入 journal 条目的
 * ASQN 区间），本模块不感知物理格式。
 *
 * <p>契约：
 *
 * <ul>
 *   <li>append 块内是完整未分片的批帧（一个追加调用一个块）；实现保证最终原子性
 *   <li>append 的调用序即 firstPosition 升序（由定序器保证）；onWrite/onCommit 按 提交序回调
 *   <li>onFailure 后该批永不 onCommit（position 区间被烧毁，读者容忍 gap）
 *   <li>{@link CommitListener#onCommit()} 无参——只表示"有新数据可读"， 具体 position 由读者自己拉取
 * </ul>
 */
public interface EventStore {

  /** 新建有状态读游标（初始位置由实现决定，通常需 seek） */
  EventStoreReader newReader();

  /**
   * 追加一个块（firstPosition..lastPosition 的批帧）
   *
   * @param firstPosition 块首条目 position
   * @param lastPosition 块末条目 position
   * @param block 批帧字节
   * @param listener 写入生命周期回调（onWrite/onCommit/onFailure）
   */
  void append(long firstPosition, long lastPosition, BufferWriter block, AppendListener listener);

  void addCommitListener(CommitListener listener);

  void removeCommitListener(CommitListener listener);

  interface AppendListener {

    /** 已写入存储（尚未提交/复制） */
    default void onWrite(long index, long lastPosition) {}

    /** 已提交（对读者可见） */
    default void onCommit(long index, long lastPosition) {}

    /** 写入/提交失败——该 position 区间作废 */
    default void onFailure(long lastPosition, Throwable cause) {}
  }

  interface CommitListener {

    /** 有新提交可读（触发 RecordAvailableListener 分发） */
    void onCommit();
  }
}
