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

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_22;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.CommandValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum ProcessInstanceBatchState implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),
  ACTIVATE_TERMINATE((short) 0, PROCESS_INDEX_55),
  ACTIVATE_TERMINATE_PROCESS((short) 1, PROCESS_INDEX_56),
  ACTIVATE_DIRECT_TERMINATE((short) 2, PROCESS_INDEX_57),
  ACTIVATE_ACTIVATE((short) 3, PROCESS_INDEX_58),
  ACTIVATE_ACTIVATE_PROCESS((short) 4, PROCESS_INDEX_59),
  ACTIVATE_DIRECT_ACTIVATE((short) 5, PROCESS_INDEX_60),
  BATCH_COMPLETE((short) 6, PROCESS_INDEX_61),
  BATCH_COMPLETED((short) 7, PROCESS_INDEX_62),
  ;

  private final short value;
  private final short processIndex;

  ProcessInstanceBatchState(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 0 -> ACTIVATE_TERMINATE;
      case 1 -> ACTIVATE_TERMINATE_PROCESS;
      case 2 -> ACTIVATE_DIRECT_TERMINATE;
      case 3 -> ACTIVATE_ACTIVATE;
      case 4 -> ACTIVATE_ACTIVATE_PROCESS;
      case 5 -> ACTIVATE_DIRECT_ACTIVATE;
      case 6 -> BATCH_COMPLETE;
      case 7 -> BATCH_COMPLETED;
      default -> UNKNOWN;
    };
  }

  public static ProcessInstanceBatchState fromValue(final short value) {
    return switch (value) {
      case 0 -> ACTIVATE_TERMINATE;
      case 1 -> ACTIVATE_TERMINATE_PROCESS;
      case 2 -> ACTIVATE_DIRECT_TERMINATE;
      case 3 -> ACTIVATE_ACTIVATE;
      case 4 -> ACTIVATE_ACTIVATE_PROCESS;
      case 5 -> ACTIVATE_DIRECT_ACTIVATE;
      case 6 -> BATCH_COMPLETE;
      case 7 -> BATCH_COMPLETED;
      default -> NULL_VAL;
    };
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
    return RECORD_INDEX_22;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.PROCESS_BATCH_INSTANCE;
  }
}
