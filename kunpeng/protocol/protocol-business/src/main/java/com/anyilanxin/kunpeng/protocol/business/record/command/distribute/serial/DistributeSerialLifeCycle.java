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
package com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_80;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.CommandValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum DistributeSerialLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  CREATE_DISTRIBUTE((short) 0, PROCESS_INDEX_194),

  CREATE_DISTRIBUTED((short) 1, PROCESS_INDEX_195),

  DISTRIBUTE_START((short) 2, PROCESS_INDEX_196),

  DISTRIBUTE_CONTINUED((short) 3, PROCESS_INDEX_197),

  DISTRIBUTE_ACK((short) 4, PROCESS_INDEX_198),

  DISTRIBUTE_ACKED((short) 5, PROCESS_INDEX_199),

  DISTRIBUTE_AFTER_START((short) 6, PROCESS_INDEX_200),

  DISTRIBUTE_AFTER_STARTED((short) 7, PROCESS_INDEX_201),

  DISTRIBUTE_AFTER_ACK((short) 8, PROCESS_INDEX_202),

  DISTRIBUTE_COMPLETE((short) 9, PROCESS_INDEX_203),

  DISTRIBUTE_COMPLETED((short) 10, PROCESS_INDEX_204);

  private final short value;
  private final short processIndex;

  DistributeSerialLifeCycle(final short value, final short processIndex) {
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
    return ValueType.DISTRIBUTE_SERIAL;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_80;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 0 -> CREATE_DISTRIBUTE;
      case 1 -> CREATE_DISTRIBUTED;
      case 2 -> DISTRIBUTE_START;
      case 3 -> DISTRIBUTE_CONTINUED;
      case 4 -> DISTRIBUTE_ACK;
      case 5 -> DISTRIBUTE_ACKED;
      case 6 -> DISTRIBUTE_AFTER_START;
      case 7 -> DISTRIBUTE_AFTER_STARTED;
      case 8 -> DISTRIBUTE_AFTER_ACK;
      case 9 -> DISTRIBUTE_COMPLETE;
      case 10 -> DISTRIBUTE_COMPLETED;
      default -> UNKNOWN;
    };
  }

  public static DistributeSerialLifeCycle fromValue(final short value) {
    return switch (value) {
      case 0 -> CREATE_DISTRIBUTE;
      case 1 -> CREATE_DISTRIBUTED;
      case 2 -> DISTRIBUTE_START;
      case 3 -> DISTRIBUTE_CONTINUED;
      case 4 -> DISTRIBUTE_ACK;
      case 5 -> DISTRIBUTE_ACKED;
      case 6 -> DISTRIBUTE_AFTER_START;
      case 7 -> DISTRIBUTE_AFTER_STARTED;
      case 8 -> DISTRIBUTE_AFTER_ACK;
      case 9 -> DISTRIBUTE_COMPLETE;
      case 10 -> DISTRIBUTE_COMPLETED;
      default -> NULL_VAL;
    };
  }
}
