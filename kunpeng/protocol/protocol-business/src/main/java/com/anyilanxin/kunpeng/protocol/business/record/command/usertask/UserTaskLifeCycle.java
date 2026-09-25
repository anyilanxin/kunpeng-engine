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

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_50;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum UserTaskLifeCycle implements ValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),
  CREATING((short) 1, PROCESS_INDEX_106),
  CREATED((short) 2, PROCESS_INDEX_107),
  UPDATING((short) 3, PROCESS_INDEX_108),
  UPDATED((short) 4, PROCESS_INDEX_109),
  ASSIGNEE((short) 5, PROCESS_INDEX_110),
  CLAIM((short) 6, PROCESS_INDEX_111),
  OWN((short) 7, PROCESS_INDEX_112),
  RESOLVE((short) 8, PROCESS_INDEX_113),
  DELEGATE((short) 9, PROCESS_INDEX_114),
  COMPLETING((short) 10, PROCESS_INDEX_115),
  COMPLETED((short) 11, PROCESS_INDEX_116),
  DELETE((short) 12, PROCESS_INDEX_117),
  DELETED((short) 13, PROCESS_INDEX_118),
  CANCEL((short) 14, PROCESS_INDEX_119),
  CANCELED((short) 15, PROCESS_INDEX_120),
  TERMINATING((short) 16, PROCESS_INDEX_121),
  TERMINATED((short) 17, PROCESS_INDEX_122),
  LISTENER_CREATE((short) 18, PROCESS_INDEX_123),
  LISTENER_COMPLETED((short) 19, PROCESS_INDEX_124),
  LISTENER_DENY((short) 20, PROCESS_INDEX_125);

  private final short value;
  private final short processIndex;

  UserTaskLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public boolean isEvent() {
    return false;
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.USER_TASK;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_50;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> UPDATING;
      case 4 -> UPDATED;
      case 5 -> ASSIGNEE;
      case 6 -> CLAIM;
      case 7 -> OWN;
      case 8 -> RESOLVE;
      case 9 -> DELEGATE;
      case 10 -> COMPLETING;
      case 11 -> COMPLETED;
      case 12 -> DELETE;
      case 13 -> DELETED;
      case 14 -> CANCEL;
      case 15 -> CANCELED;
      case 16 -> TERMINATING;
      case 17 -> TERMINATED;
      case 18 -> LISTENER_CREATE;
      case 19 -> LISTENER_COMPLETED;
      case 20 -> LISTENER_DENY;
      default -> UNKNOWN;
    };
  }

  public static UserTaskLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> UPDATING;
      case 4 -> UPDATED;
      case 5 -> ASSIGNEE;
      case 6 -> CLAIM;
      case 7 -> OWN;
      case 8 -> RESOLVE;
      case 9 -> DELEGATE;
      case 10 -> COMPLETING;
      case 11 -> COMPLETED;
      case 12 -> DELETE;
      case 13 -> DELETED;
      case 14 -> CANCEL;
      case 15 -> CANCELED;
      case 16 -> TERMINATING;
      case 17 -> TERMINATED;
      case 18 -> LISTENER_CREATE;
      case 19 -> LISTENER_COMPLETED;
      default -> NULL_VAL;
    };
  }
}
