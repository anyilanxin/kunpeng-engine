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

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum ValueType {
  UNKNOW((short) -1),
  // 部署相关
  DEPLOYMENT_API((short) 0),
  DEPLOYMENT((short) 1),
  PROCESS_DEFINITION_API((short) 2),
  PROCESS_DEFINITION((short) 3),
  DECISION_DEFINITION((short) 4),
  DECISION_REQUIREMENTS((short) 5),
  FORM_DEFINITION((short) 6),
  RESOURCE_DEFINITION((short) 7),

  PROCESS_INSTANCE_API((short) 8),
  PROCESS_INSTANCE((short) 9),
  PROCESS_BATCH_INSTANCE((short) 10),
  DISTRIBUTE_SERIAL((short) 11),
  DISTRIBUTE_PARALLEL((short) 12),
  ACTIVITY((short) 13),
  VARIABLE_API((short) 14),
  VARIABLE((short) 15),
  USER_TASK_API((short) 16),
  USER_TASK((short) 17),
  INCIDENT_API((short) 18),
  INCIDENT((short) 19),
  ASYNC_REQUEST((short) 20),
  JOB_API((short) 21),
  JOB((short) 22),
  JOB_BATCH_API((short) 23),
  JOB_BATCH((short) 24),
  MESSAGE_SUBSCRIPTION_API((short) 25),
  MESSAGE_SUBSCRIPTION((short) 26),
  MESSAGE_DISTRIBUTE_CORRELATE((short) 27),
  SIGNAL_SUBSCRIPTION_API((short) 28),
  SIGNAL_SUBSCRIPTION((short) 29),
  SIGNAL_DISTRIBUTE_CORRELATE((short) 30),

  TIMER_API((short) 31),
  TIMER((short) 32),

  DELAY_EVENT_COMMAND((short) 35),
  HISTORY_CLEANUP((short) 36),

  // 查询部分
  QUERY_BUSINESS_ROUTE((short) 38),
  RESPONSE_BUSINESS_ROUTE((short) 39),
  EMPTY((short) 52),
  ;

  private final short value;

  ValueType(final short value) {
    this.value = value;
  }

  public short getValue() {
    return value;
  }

  public static ValueType valueOf(final short value) {
    for (final ValueType type : ValueType.values()) {
      if (type.value == value) {
        return type;
      }
    }
    return UNKNOW;
  }
}
