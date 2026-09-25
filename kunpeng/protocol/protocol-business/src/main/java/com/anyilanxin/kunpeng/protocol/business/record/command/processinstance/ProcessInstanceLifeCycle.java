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

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_21;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum ProcessInstanceLifeCycle implements ValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),
  ACTIVATING((short) 1, PROCESS_INDEX_43),
  ACTIVATED((short) 2, PROCESS_INDEX_44),
  SUSPENDED((short) 3, PROCESS_INDEX_45),
  COMPLETING((short) 4, PROCESS_INDEX_46),
  COMPLETED((short) 5, PROCESS_INDEX_47),
  CANCEL((short) 6, PROCESS_INDEX_48),
  CANCELED((short) 7, PROCESS_INDEX_49),
  TERMINATING((short) 8, PROCESS_INDEX_50),
  TERMINATED((short) 9, PROCESS_INDEX_51),
  LISTENER_CREATE((short) 10, PROCESS_INDEX_52),
  LISTENER_COMPLETED((short) 11, PROCESS_INDEX_53),
  LISTENER_DENY((short) 12, PROCESS_INDEX_54);

  private final short value;
  private final short processIndex;

  ProcessInstanceLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> ACTIVATING;
      case 2 -> ACTIVATED;
      case 3 -> SUSPENDED;
      case 4 -> COMPLETING;
      case 5 -> COMPLETED;
      case 6 -> CANCEL;
      case 7 -> CANCELED;
      case 8 -> TERMINATING;
      case 9 -> TERMINATED;
      case 10 -> LISTENER_CREATE;
      case 11 -> LISTENER_COMPLETED;
      case 12 -> LISTENER_DENY;
      default -> UNKNOWN;
    };
  }

  public static ProcessInstanceLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> ACTIVATING;
      case 2 -> ACTIVATED;
      case 3 -> SUSPENDED;
      case 4 -> COMPLETING;
      case 5 -> COMPLETED;
      case 6 -> CANCEL;
      case 7 -> CANCELED;
      case 8 -> TERMINATING;
      case 9 -> TERMINATED;
      case 10 -> LISTENER_CREATE;
      case 11 -> LISTENER_COMPLETED;
      case 12 -> LISTENER_DENY;
      default -> NULL_VAL;
    };
  }

  @Override
  public ValueType getValueType() {
    return ValueType.PROCESS_INSTANCE;
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
  public short recordIndex() {
    return RECORD_INDEX_21;
  }
}
