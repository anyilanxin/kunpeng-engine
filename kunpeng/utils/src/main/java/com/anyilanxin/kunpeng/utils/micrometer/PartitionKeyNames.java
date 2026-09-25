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
package com.anyilanxin.kunpeng.utils.micrometer;

import io.micrometer.core.instrument.Tags;

/**
 * 与分区维度相关的指标标签约定。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum PartitionKeyNames {
  /** 分区号标签键。 */
  PARTITION("partition");

  private final String key;

  PartitionKeyNames(final String key) {
    this.key = key;
  }

  /** 标签键名。 */
  public String getKey() {
    return key;
  }

  /**
   * 构造带分区号的标签。
   *
   * @param partitionId 分区号
   * @return 形如 {@code partition=<partitionId>} 的标签
   */
  public static Tags tags(final int partitionId) {
    return Tags.of("partition", String.valueOf(partitionId));
  }
}
