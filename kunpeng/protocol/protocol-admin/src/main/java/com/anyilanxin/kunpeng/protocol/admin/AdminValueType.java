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
package com.anyilanxin.kunpeng.protocol.admin;

/**
 * 管理协议值类型枚举。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum AdminValueType {
  // 协议值必须全局唯一：AdminValueType.valueOf(short) 按声明顺序返回第一个匹配，
  // 重复值会导致 RecordMetadata 往返解码后 valueType 串类型
  UNKNOW((short) -1),
  // 部署相关
  ADMIN_DISPATCH_EXECUTION((short) 0),
  ADMIN_DISPATCH_API((short) 1),
  ADMIN_DISPATCH((short) 2),

  BUSINESS_DISPATCH_EXECUTION((short) 3),
  BUSINESS_DISPATCH_API((short) 4),
  BUSINESS_DISPATCH((short) 5),

  ADMIN_CLUSTER_META((short) 6),
  BUSINESS_CLUSTER_META((short) 7),

  DELAYED((short) 8),

  NODE_SOURCE_META((short) 9),
  NODE_SOURCE((short) 10),
  PARTITION_SOURCE_META((short) 11),
  PARTITION_SOURCE((short) 12),
  ;

  private final short value;

  AdminValueType(final short value) {
    this.value = value;
  }

  public short getValue() {
    return value;
  }

  public static AdminValueType valueOf(final short value) {
    for (final AdminValueType type : AdminValueType.values()) {
      if (type.value == value) {
        return type;
      }
    }
    return UNKNOW;
  }
}
