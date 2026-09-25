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
package com.anyilanxin.kunpeng.protocol.business.record.command.timer;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_68;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum TimerLifeCycle implements ValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),
  CREATING((short) 1, PROCESS_INDEX_167),
  CREATED((short) 2, PROCESS_INDEX_168),
  TRIGGER((short) 3, PROCESS_INDEX_169),
  TRIGGERED((short) 4, PROCESS_INDEX_170),
  CANCEL((short) 5, PROCESS_INDEX_171),
  CANCELED((short) 6, PROCESS_INDEX_172),
  ;

  private final short value;
  private final short processIndex;

  TimerLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> TRIGGER;
      case 4 -> TRIGGERED;
      case 5 -> CANCEL;
      case 6 -> CANCELED;
      default -> UNKNOWN;
    };
  }

  public static TimerLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> TRIGGER;
      case 4 -> TRIGGERED;
      case 5 -> CANCEL;
      case 6 -> CANCELED;
      default -> NULL_VAL;
    };
  }

  @Override
  public ValueType getValueType() {
    return ValueType.TIMER;
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
    return RECORD_INDEX_68;
  }
}
