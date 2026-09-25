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
package com.anyilanxin.kunpeng.protocol.business.record.command.usertask;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum UserTaskState {
  /** 初始/待处理：任务已创建，等待被认领或分配 */
  PENDING("PENDING", "待处理"),

  /** 活跃/处理中：任务已被用户认领，正在处理 */
  ACTIVE("ACTIVE", "处理中"),

  /** 已完成：任务已成功处理完毕 */
  COMPLETING("COMPLETED", "已完成"),

  COMPLETED("COMPLETED", "已完成"),

  /** 已取消/终止：任务被主动终止，不再执行 */
  CANCEL("CANCELLED", "已取消"),

  CANCELED("CANCELLED", "已取消"),

  /** 已取消/终止：任务被主动终止，不再执行 */
  TERMINATING("CANCELLED", "已取消"),

  TERMINATED("CANCELLED", "已取消"),

  /** 已挂起/暂停：任务被临时暂停，等待条件满足 */
  SUSPENDED("SUSPENDED", "已暂停"),

  /** 已超时：任务未在规定时间内完成(只有认领后才会触发超时) */
  TIMEOUT("TIMEOUT", "已超时");

  private final String value;
  private final String describe;

  UserTaskState(final String value, final String describe) {
    this.value = value;
    this.describe = describe;
  }

  public String value() {
    return value;
  }

  public String describe() {
    return describe;
  }
}
