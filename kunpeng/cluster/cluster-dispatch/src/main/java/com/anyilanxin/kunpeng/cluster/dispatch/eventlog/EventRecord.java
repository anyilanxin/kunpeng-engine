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
package com.anyilanxin.kunpeng.cluster.dispatch.eventlog;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordType;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import com.anyilanxin.kunpeng.structpack.JsonSerializable;

/** 表示发布到日志流（log stream）的一条记录。 */
public interface EventRecord<T extends RecordValue> extends JsonSerializable {
  /**
   * 获取记录的位置（position）。position 在分区范围内唯一且单调递增。分区内的记录按 position 排序， 即 position 越小表示该记录发布得越早。
   *
   * @return 记录的 position
   */
  long getPosition();

  /**
   * 返回源记录（source record）的 position。源记录指引发当前记录的那条记录。该值可以未设置（即不存在 源记录），此时返回的 position 为 -1；任何 >= 0
   * 的值都表示存在源记录。
   *
   * @return 源记录的 position
   */
  long getSourceRecordPosition();

  /**
   * 获取记录的键（key）。
   *
   * <p>属于同一逻辑实体的多条记录可以拥有相同的 key。key 在「分区 + 记录类型」的组合内唯一。
   *
   * @return 记录的 key
   */
  long getKey();

  /**
   * @return 记录发布到分区时的 Unix 时间戳。
   */
  long getTimestamp();

  /**
   * @return the type of the record (event, command or command rejection)
   */
  RecordType getRecordType();

  /**
   * @return 记录的生命周期状态
   */
  AdminValueLifeCycle getValueState();

  /**
   * @return 记录发布到的分区 ID
   */
  int getResourceId();

  String getRejectionType();

  String getRejectionReason();

  /**
   * @return 写入该记录的 Broker 版本
   */
  String getBrokerVersion();

  /**
   * 记录版本（record version）是一个从 1 开始的整数，在记录写入时确定。它允许同一记录的不同版本被不同地处理或应用。
   *
   * <p>例如，旧版本的事件仍按其最初写入时的方式应用，而新版本的事件可以按新的方式应用。
   *
   * @return 记录写入时的版本号
   */
  int getRecordVersion();

  /**
   * @return 记录对应的值类型（如 Job、Process、ProcessInstance 等）
   */
  AdminValueType getValueType();

  /**
   * 返回记录的原始值，应为 {@link io.camunda.zeebe.protocol.record.value} 包中某个接口的实现。
   *
   * <p>记录值本质上是记录特有的数据，例如对于流程实例创建事件，其中包含与被创建流程实例相关的信息。
   *
   * @return 记录值
   */
  T getValue();

  /**
   * 创建当前记录的深拷贝。可用于收集记录。
   *
   * @return 当前记录的一个深拷贝
   */
  default EventRecord<T> copyOf() {
    throw new UnsupportedOperationException(
        "Failed to create a deep copy of this record; this implementation does not support this out"
            + " of the box");
  }
}
