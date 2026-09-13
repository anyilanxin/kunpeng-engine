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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;

/**
 * 按 {@link AdminValueLifeCycle lifeCycle} 分组去重在途命令 key。
 *
 * <p>{@link Stageable} 注册表可在 {@link Staging} 会话中缓冲一批变更：暂存的变更对主缓存 不可见，直到 {@link Staging#commit()}
 * 将其合并；{@link Staging#rollback()} 则整体丢弃。 暂存会话内，{@link Staging#contains} 先查缓冲、再回落到主缓存。
 */
public interface PendingCommandRegistry {

  /** 记录 {@code lifeCycle} 下 {@code key} 的命令已进入在途状态。 */
  void add(AdminValueLifeCycle lifeCycle, long key);

  /** 返回 {@code lifeCycle} 下 {@code key} 的命令是否已在途。 */
  boolean contains(AdminValueLifeCycle lifeCycle, long key);

  /** 遗忘 {@code lifeCycle} 下 {@code key} 的在途命令。 */
  void remove(AdminValueLifeCycle lifeCycle, long key);

  /** 遗忘所有在途命令。 */
  void clear();

  /** 支持将一批变更暂存后再生效的注册表。 */
  interface Stageable extends PendingCommandRegistry {

    /** 基于本注册表开启一个新的暂存会话。 */
    Staging stage();
  }

  /**
   * {@link Stageable} 注册表的缓冲视图。
   *
   * <p>提交前变更只触碰缓冲；读取先查缓冲、再查主缓存。非线程安全。
   */
  interface Staging extends PendingCommandRegistry {

    /** 将所有暂存变更合并进主缓存。 */
    void commit();

    /** 丢弃所有暂存变更，不触碰主缓存。 */
    void rollback();
  }
}
