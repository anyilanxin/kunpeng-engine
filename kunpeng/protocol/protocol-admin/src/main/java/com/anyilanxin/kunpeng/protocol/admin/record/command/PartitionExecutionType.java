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
package com.anyilanxin.kunpeng.protocol.admin.record.command;

/**
 * 管理分区扩缩容执行类型枚举，作为管理 Raft 执行服务消息处理的操作标识。
 *
 * @author zxuanhong
 * @since 2026.9.0
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
  /** 临时冻结分区（数据手术前置：关写入入口、停内部触发、两段位点排水，见 cluster-dispatch-design 03 号文档） */
  FREEZE(11),
  /** 解冻分区（数据手术收尾：重扫补触发、开写入入口、清除不可用通告） */
  RESUME(12),
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
