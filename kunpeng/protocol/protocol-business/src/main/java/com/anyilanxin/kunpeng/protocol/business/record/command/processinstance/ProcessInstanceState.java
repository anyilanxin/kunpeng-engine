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
package com.anyilanxin.kunpeng.protocol.business.record.command.processinstance;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum ProcessInstanceState {
  /** 活动 */
  ACTIVATED("ACTIVATED", "活动"),

  /** 暂停 */
  SUSPENDED("SUSPENDED", "暂停"),
  /** 完成 */
  COMPLETED("COMPLETED", "完成"),
  /** 终止中 */
  TERMINATING("TERMINATING", "终止中"),
  /** 终止 */
  TERMINATED("TERMINATED", "终止"),
  /** 取消中 */
  CANCEL("CANCEL", "取消中"),
  /** 取消 */
  CANCELED("CANCELED", "取消"),
  ;

  private final String value;
  private final String describe;

  ProcessInstanceState(final String value, final String describe) {
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
