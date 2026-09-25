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
package com.anyilanxin.kunpeng.protocol.business.record.command.variable;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_31;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.CommandValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum VariableLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),
  CREATE((short) 0, PROCESS_INDEX_92),
  CREATED((short) 1, PROCESS_INDEX_93),
  UPDATE((short) 2, PROCESS_INDEX_94),
  UPDATED((short) 3, PROCESS_INDEX_95),
  REMOVE((short) 4, PROCESS_INDEX_96),
  REMOVED((short) 5, PROCESS_INDEX_90),
  HISTORY((short) 6, PROCESS_INDEX_213),
  ;

  private final short value;
  private final short processIndex;

  VariableLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public boolean isEvent() {
    return true;
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.VARIABLE;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_31;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 0 -> CREATE;
      case 1 -> CREATED;
      case 2 -> UPDATE;
      case 3 -> UPDATED;
      case 4 -> REMOVE;
      case 5 -> REMOVED;
      case 6 -> HISTORY;
      default -> UnknownState.UNKNOWN;
    };
  }

  public static VariableLifeCycle fromValue(final short value) {
    return switch (value) {
      case 0 -> CREATE;
      case 1 -> CREATED;
      case 2 -> UPDATE;
      case 3 -> UPDATED;
      case 4 -> REMOVE;
      case 5 -> REMOVED;
      case 6 -> HISTORY;
      default -> CREATE;
    };
  }
}
