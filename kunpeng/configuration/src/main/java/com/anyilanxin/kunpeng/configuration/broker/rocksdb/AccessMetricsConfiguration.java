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
package com.anyilanxin.kunpeng.configuration.broker.rocksdb;

/**
 * RocksDB 访问指标采集配置。
 *
 * @param kind 采集级别：{@link Kind#NONE} 不采集，{@link Kind#FINE} 细粒度采集
 * @param partitionId 关联的分区 ID
 * @author zxuanhong
 * @since
 */
public record AccessMetricsConfiguration(Kind kind, int partitionId) {

  /** 访问指标采集级别 */
  public enum Kind {
    /** 不采集 */
    NONE,
    /** 细粒度采集 */
    FINE
  }
}
