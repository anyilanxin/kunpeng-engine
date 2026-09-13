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
import com.anyilanxin.kunpeng.protocol.admin.impl.FollowUpCommandMetadata;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import java.util.List;

/**
 * 缓冲 {@link TimerJob} 运行期间产出的命令，并将其组装为 {@link CommandBatch}。
 *
 * <p>各重载仅为便利而设：不关心 key 的调用方可省略 key，不关心后续元数据的调用方也可 将其省略。
 */
public interface CommandCollector {

  /** 命令未关联 key 时使用的哨兵值。 */
  long NO_KEY = -1L;

  /**
   * 追加命令，类型取自生命周期，key 使用 {@link #NO_KEY}。
   *
   * @return 命令被接受时返回 true
   */
  default boolean appendCommand(
      final AdminValueLifeCycle lifeCycle, final UnifiedRecordValue value) {
    return appendCommand(NO_KEY, lifeCycle, value);
  }

  /**
   * 以给定类型与 key 追加命令，不带后续元数据。
   *
   * @return 命令被接受时返回 true
   */
  default boolean appendCommand(
      final long key, final AdminValueLifeCycle lifeCycle, final UnifiedRecordValue value) {
    return appendCommand(key, lifeCycle, value, FollowUpCommandMetadata.empty());
  }

  /**
   * 以完整指定的类型、key、生命周期、值与后续元数据追加命令。
   *
   * @return 命令被接受时返回 true
   */
  boolean appendCommand(
      long key,
      AdminValueLifeCycle lifeCycle,
      UnifiedRecordValue value,
      FollowUpCommandMetadata metadata);

  /**
   * 返回在给定后续元数据下是否仍可追加这些值。
   *
   * @param values 候选值
   * @param metadata 它们将携带的后续元数据
   * @return 仍有空间容纳时返回 true
   */
  boolean canAppend(List<? extends UnifiedRecordValue> values, FollowUpCommandMetadata metadata);

  /** 将已收集的命令定稿为单个批次。 */
  CommandBatch build();
}
