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
package com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_24;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.CommandValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum ActivityInstanceLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  ACTIVATING((short) 1, PROCESS_INDEX_74),

  ACTIVATING_AFTER((short) 2, PROCESS_INDEX_75),

  ACTIVATED((short) 3, PROCESS_INDEX_76),

  OCCURRED((short) 4, PROCESS_INDEX_77),

  TAKING((short) 5, PROCESS_INDEX_78),

  TAKEN((short) 6, PROCESS_INDEX_79),

  COMPLETING((short) 7, PROCESS_INDEX_80),

  COMPLETING_AFTER((short) 8, PROCESS_INDEX_81),

  COMPLETED((short) 9, PROCESS_INDEX_82),
  TERMINATING((short) 10, PROCESS_INDEX_83),

  TERMINATING_AFTER((short) 11, PROCESS_INDEX_84),

  TERMINATED((short) 12, PROCESS_INDEX_85),
  LISTENER_CREATE((short) 13, PROCESS_INDEX_86),
  LISTENER_COMPLETED((short) 14, PROCESS_INDEX_87),

  LISTENER_DENY((short) 15, PROCESS_INDEX_88);

  private final short value;
  private final short processIndex;

  ActivityInstanceLifeCycle(final short value, final short processIndex) {
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
    return ValueType.ACTIVITY;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_24;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> ACTIVATING;
      case 2 -> ACTIVATING_AFTER;
      case 3 -> ACTIVATED;
      case 4 -> OCCURRED;
      case 5 -> TAKING;
      case 6 -> TAKEN;
      case 7 -> COMPLETING;
      case 8 -> COMPLETING_AFTER;
      case 9 -> COMPLETED;
      case 10 -> TERMINATING;
      case 11 -> TERMINATING_AFTER;
      case 12 -> TERMINATED;
      case 13 -> LISTENER_CREATE;
      case 14 -> LISTENER_COMPLETED;
      case 15 -> LISTENER_DENY;
      default -> UNKNOWN;
    };
  }

  public static ActivityInstanceLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> ACTIVATING;
      case 2 -> ACTIVATING_AFTER;
      case 3 -> ACTIVATED;
      case 4 -> OCCURRED;
      case 5 -> TAKING;
      case 6 -> TAKEN;
      case 7 -> COMPLETING;
      case 8 -> COMPLETING_AFTER;
      case 9 -> COMPLETED;
      case 10 -> TERMINATING;
      case 11 -> TERMINATING_AFTER;
      case 12 -> TERMINATED;
      case 13 -> LISTENER_CREATE;
      case 14 -> LISTENER_COMPLETED;
      case 15 -> LISTENER_DENY;
      default -> NULL_VAL;
    };
  }
}
