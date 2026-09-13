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
package com.anyilanxin.kunpeng.protocol.admin.record.command;

/**
 * 管理分区扩缩容执行类型枚举，作为管理 Raft 执行服务消息处理的操作标识。
 *
 * @author zxuanhong
 * @since
 */
public enum PartitionExecutionType {
  /** 引导分区 */
  BOOTSTRAP(0),
  /** 引导分区资源数据转移 */
  BOOTSTRAP_SOURCE_DATA_TRANSFER(1),
  /** 引导分区资源标识转移 */
  BOOTSTRAP_SOURCE_TRANSFER(2),
  /** 加入分区 */
  JOIN(3),
  /** 离开分区 */
  LEAVE(4),
  /** 离开分区资源数据转移 */
  LEAVE_SOURCE_DATA_TRANSFER(5),
  /** 离开分区资源标识转移 */
  LEAVE_SOURCE_TRANSFER(6),
  /** 停止分区（分区最后一个成员本地停止并销毁分区，不走 leave 协议） */
  STOP(7),
  /** 修改分区配置 */
  CONFIG_CHANGE(8),
  /** 分区资源数据转移 */
  SOURCE_DATA_TRANSFER(9),
  /** 分区资源标识转移 */
  SOURCE_TRANSFER(10),
  ;

  /** 全部成员中最大的 value，类加载时计算并缓存 */
  private static final int MAX_VALUE = computeMaxValue();

  final int value;

  PartitionExecutionType(final int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  /** 返回全部成员中最大的 value（以显式声明的 value 为准，而不是枚举个数，避免未来 value 跳号后误判） */
  public static int getMaxValue() {
    return MAX_VALUE;
  }

  private static int computeMaxValue() {
    int maxValue = Integer.MIN_VALUE;
    for (final PartitionExecutionType type : values()) {
      if (type.value > maxValue) {
        maxValue = type.value;
      }
    }
    return maxValue;
  }

  public static String getTopic(
      final PartitionType partitionType, final PartitionExecutionType executionType) {
    return "%s-%s".formatted(partitionType.name(), executionType.name());
  }
}
