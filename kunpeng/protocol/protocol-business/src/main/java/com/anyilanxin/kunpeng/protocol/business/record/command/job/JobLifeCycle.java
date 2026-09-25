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
package com.anyilanxin.kunpeng.protocol.business.record.command.job;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_61;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum JobLifeCycle implements ValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),
  CREATING((short) 1, PROCESS_INDEX_137),
  CREATED((short) 2, PROCESS_INDEX_138),
  COMPLETING((short) 3, PROCESS_INDEX_139),
  COMPLETED((short) 4, PROCESS_INDEX_140),
  UPDATING((short) 5, PROCESS_INDEX_141),
  UPDATED((short) 6, PROCESS_INDEX_142),
  REFUSING((short) 8, PROCESS_INDEX_143),
  REFUSED((short) 9, PROCESS_INDEX_144),
  WITHDRAW((short) 10, PROCESS_INDEX_145),
  TIME_OUT((short) 11, PROCESS_INDEX_146),
  TIMED_OUT((short) 12, PROCESS_INDEX_147),
  ;

  private final short value;
  private final short processIndex;

  JobLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> COMPLETING;
      case 4 -> COMPLETED;
      case 5 -> UPDATING;
      case 6 -> UPDATED;
      case 8 -> REFUSING;
      case 9 -> REFUSED;
      case 10 -> WITHDRAW;
      case 12 -> TIMED_OUT;
      default -> UNKNOWN;
    };
  }

  public static JobLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> COMPLETING;
      case 4 -> COMPLETED;
      case 5 -> UPDATING;
      case 6 -> UPDATED;
      case 8 -> REFUSING;
      case 9 -> REFUSED;
      case 10 -> WITHDRAW;
      case 12 -> TIMED_OUT;
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
  public ValueType getValueType() {
    return ValueType.JOB;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_61;
  }
}
