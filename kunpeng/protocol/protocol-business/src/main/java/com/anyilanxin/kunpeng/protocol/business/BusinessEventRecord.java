/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.protocol.business;

import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import com.anyilanxin.kunpeng.structpack.JsonSerializable;

/**
 * 表示发布到日志流（log stream）的一条记录。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface BusinessEventRecord<T extends RecordValue> extends JsonSerializable {
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
   * @return 记录类型（事件、命令或命令拒绝）
   */
  RecordType getRecordType();

  /**
   * @return 记录的生命周期状态
   */
  ValueLifeCycle getValueState();

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
  ValueType getValueType();

  /**
   * 返回记录的原始值，为本协议各值类型接口（record value 包）的某个实现。
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
  default BusinessEventRecord<T> copyOf() {
    throw new UnsupportedOperationException(
        "Failed to create a deep copy of this record; this implementation does not support this out"
            + " of the box");
  }
}
