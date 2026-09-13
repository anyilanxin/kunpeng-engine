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

import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * 一次 {@link TimerJob} 运行期间收集到的命令批次（只读视图）。
 *
 * <p>批次可按条目迭代，也可经 {@link #entries()} 取得可写入事件日志的待追加条目列表。 构建完成后批次不再暴露产出它的收集器。可变实现见 {@link
 * HeapCommandBatch}。
 */
public interface CommandBatch extends Iterable<CommandRecord> {

  /**
   * 判定下一批条目能否入批的容量探针。
   *
   * <p>入参依次为潜在的新条目数与累加后的新总字节数，返回 false 表示超限、拒绝追加。
   */
  @FunctionalInterface
  interface CapacityProbe extends BiPredicate<Integer, Integer> {}

  /** 返回 true：批内没有待追加条目。 */
  default boolean isEmpty() {
    return entries().isEmpty();
  }

  /** 返回本批收集的全部待追加条目（不可修改视图）。 */
  List<AppendEntry> entries();
}
