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
package com.anyilanxin.kunpeng.protocol.business.record.command.deployment;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_11;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.CommandValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum ProcessDefinitionLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),
  CREATE((short) 0, PROCESS_INDEX_15),
  CREATED((short) 1, PROCESS_INDEX_16),
  CREATE_DISTRIBUTE((short) 2, PROCESS_INDEX_17),
  CREATED_DISTRIBUTE((short) 3, PROCESS_INDEX_18),
  ACTIVATE((short) 4, PROCESS_INDEX_19),
  ACTIVATE_DISTRIBUTE((short) 5, PROCESS_INDEX_20),
  ACTIVATED((short) 6, PROCESS_INDEX_21),
  ACTIVATED_DISTRIBUTE((short) 7, PROCESS_INDEX_22),
  ACTIVATED_DISTRIBUTE_AFTER((short) 8, PROCESS_INDEX_23),
  SUSPEND((short) 9, PROCESS_INDEX_24),
  SUSPEND_DISTRIBUTE((short) 10, PROCESS_INDEX_25),
  SUSPENDED((short) 11, PROCESS_INDEX_26),
  SUSPENDED_DISTRIBUTE((short) 12, PROCESS_INDEX_27),
  SUSPENDED_DISTRIBUTE_AFTER((short) 13, PROCESS_INDEX_28),
  DELETE((short) 14, PROCESS_INDEX_29),
  DELETE_DISTRIBUTE((short) 15, PROCESS_INDEX_30),
  DELETED((short) 16, PROCESS_INDEX_31),
  DELETED_DISTRIBUTE((short) 17, PROCESS_INDEX_32),
  DELETED_DISTRIBUTE_AFTER((short) 18, PROCESS_INDEX_33);

  private final short value;
  private final short processIndex;

  ProcessDefinitionLifeCycle(final short value, final short processIndex) {
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
  public short recordIndex() {
    return RECORD_INDEX_11;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.PROCESS_DEFINITION;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 0 -> CREATE;
      case 1 -> CREATED;
      case 2 -> CREATE_DISTRIBUTE;
      case 3 -> CREATED_DISTRIBUTE;
      case 4 -> ACTIVATE;
      case 5 -> ACTIVATE_DISTRIBUTE;
      case 6 -> ACTIVATED;
      case 7 -> ACTIVATED_DISTRIBUTE;
      case 8 -> ACTIVATED_DISTRIBUTE_AFTER;
      case 9 -> SUSPEND;
      case 10 -> SUSPEND_DISTRIBUTE;
      case 11 -> SUSPENDED;
      case 12 -> SUSPENDED_DISTRIBUTE;
      case 13 -> SUSPENDED_DISTRIBUTE_AFTER;
      case 14 -> DELETE;
      case 15 -> DELETE_DISTRIBUTE;
      case 16 -> DELETED;
      case 17 -> DELETED_DISTRIBUTE;
      case 18 -> DELETED_DISTRIBUTE_AFTER;
      default -> UNKNOWN;
    };
  }

  public static ProcessDefinitionLifeCycle fromValue(final short value) {
    return switch (value) {
      case 0 -> CREATE;
      case 1 -> CREATED;
      case 2 -> CREATE_DISTRIBUTE;
      case 3 -> CREATED_DISTRIBUTE;
      case 4 -> ACTIVATE;
      case 5 -> ACTIVATE_DISTRIBUTE;
      case 6 -> ACTIVATED;
      case 7 -> ACTIVATED_DISTRIBUTE;
      case 8 -> ACTIVATED_DISTRIBUTE_AFTER;
      case 9 -> SUSPEND;
      case 10 -> SUSPEND_DISTRIBUTE;
      case 11 -> SUSPENDED;
      case 12 -> SUSPENDED_DISTRIBUTE;
      case 13 -> SUSPENDED_DISTRIBUTE_AFTER;
      case 14 -> DELETE;
      case 15 -> DELETE_DISTRIBUTE;
      case 16 -> DELETED;
      case 17 -> DELETED_DISTRIBUTE;
      case 18 -> DELETED_DISTRIBUTE_AFTER;
      default -> NULL_VAL;
    };
  }
}
