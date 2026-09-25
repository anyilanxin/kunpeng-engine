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
package com.anyilanxin.kunpeng.sink.runtime;

/**
 * 描述新启用 Sink 的持久化状态应如何初始化。
 *
 * @param metadataVersion 初始化规则的版本号；已存储的版本不低于该值时，说明状态此前已初始化过，保持不动
 * @param inheritStateFrom 另一个 Sink 的 id，其位置与元数据将作为本 Sink 状态的种子； 典型场景是重命名后接续同名 Sink 的前身；为 {@code
 *     null} 时从日志开头开始
 * @author zxuanhong
 * @since 2026.9.0
 */
public record SinkInitInfo(long metadataVersion, String inheritStateFrom) {

  /** 从零初始化：位置 -1，无元数据。 */
  public static SinkInitInfo fresh() {
    return new SinkInitInfo(0, null);
  }

  /** 用另一个 Sink 的当前状态初始化本 Sink 。 */
  public static SinkInitInfo inheritFrom(final String sinkId) {
    return new SinkInitInfo(0, sinkId);
  }
}
