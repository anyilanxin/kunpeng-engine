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
package com.anyilanxin.kunpeng.cluster.raft.metadata;

import java.util.Map;

/**
 * 分区业务元数据的内存存储类型：内部 {@code Map<String, String>} 存放键值对，具体内容由调用方决定（如 sourceId）。
 *
 * <p>apply 为 copy-on-write：整体替换不可变 map 引用，读方（含快照拍摄线程）无锁读取一致性视图； appliedIndex 单调递增保证重复/乱序回调幂等。
 */
public final class PartitionBusinessMeta {

  private volatile Map<String, String> entries = Map.of();
  private volatile long appliedIndex = -1;
  private volatile long appliedTerm = -1;

  /** 应用一条已提交 busimeta 条目；index 不大于当前值时忽略（幂等）。 */
  public synchronized void apply(
      final long index, final long term, final Map<String, String> newEntries) {
    if (index <= appliedIndex) {
      return;
    }
    entries = newEntries == null ? Map.of() : Map.copyOf(newEntries);
    appliedIndex = index;
    appliedTerm = term;
  }

  /** 已提交状态的一致性视图（index/term/entries 三元组原子读取）。 */
  public record AppliedState(long index, long term, Map<String, String> entries) {}

  /** 原子读取当前已提交状态（供快照拍摄等需要 index 与 entries 严格配对的读方）。 */
  public synchronized AppliedState appliedState() {
    return new AppliedState(appliedIndex, appliedTerm, entries);
  }

  /** 全部键值对只读视图。 */
  public Map<String, String> entries() {
    return entries;
  }

  /** 最近一次应用的已提交条目 index，未应用为 -1。 */
  public long appliedIndex() {
    return appliedIndex;
  }

  /** 最近一次应用的已提交条目 term，未应用为 -1。 */
  public long appliedTerm() {
    return appliedTerm;
  }
}
