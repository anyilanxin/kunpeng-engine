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
package com.anyilanxin.kunpeng.sink.rdbms.write;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * flush 窗口内的待写变更集合，只在 sink 线程上访问，无需任何并发控制。
 *
 * <p>合并规则以「形态 + 表 + 主键」为键：同一实体的连续 update 在窗口内只剩最后一笔；insert 与 update
 * 各自独立并保持入队顺序，同一事务内先插后更，数据库自然收敛到最终状态。一次 flush 的语句数与窗口内 去重后的变更数线性相关，而不是与事件数相关。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ChangeBuffer {

  private final Map<String, RowChange> pending = new LinkedHashMap<>();
  private long lastPosition = -1L;

  /** 记录一条变更，同键（同形态同主键）变更被替换。 */
  public void offer(final RowChange change) {
    pending.put(change.coalesceKey(), change);
  }

  /** 记录最近处理到的日志位置；无论记录是否产出变更都必须推进，否则确认点会滞后。 */
  public void advancePosition(final long position) {
    lastPosition = position;
  }

  /**
   * @return 窗口内最近一次推进到的位置
   */
  public long lastPosition() {
    return lastPosition;
  }

  /**
   * @return 当前待写变更数（同形态同主键已去重）
   */
  public int size() {
    return pending.size();
  }

  /**
   * @return 是否没有任何待写变更
   */
  public boolean isEmpty() {
    return pending.isEmpty();
  }

  /**
   * 只读视图，供 flush 执行；内容仅在 {@link #clear()} 之后才会真正丢弃，因此 flush 失败时缓冲保持原样， 重试或记录重投递都能基于同一份内容继续。
   *
   * @return 待写变更（保持入队顺序）
   */
  public Collection<RowChange> view() {
    return pending.values();
  }

  /** flush 成功提交后清空缓冲。 */
  public void clear() {
    pending.clear();
  }
}
