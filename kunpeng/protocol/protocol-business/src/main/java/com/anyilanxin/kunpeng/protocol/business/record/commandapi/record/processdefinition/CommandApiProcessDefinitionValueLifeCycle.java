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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processdefinition;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_10;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_5;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_6;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_7;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_8;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_9;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.CommandApiValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum CommandApiProcessDefinitionValueLifeCycle implements CommandApiValueLifeCycle {
  ACTIVATE_REQUEST((short) 0, PROCESS_INDEX_12, RECORD_INDEX_5),
  ACTIVATE_RESPONSE((short) 1, NOT_PROCESS_INDEX, RECORD_INDEX_6),
  DELETE_REQUEST((short) 2, PROCESS_INDEX_13, RECORD_INDEX_7),
  DELETE_RESPONSE((short) 3, NOT_PROCESS_INDEX, RECORD_INDEX_8),
  SUSPEND_REQUEST((short) 4, PROCESS_INDEX_14, RECORD_INDEX_9),
  SUSPEND_RESPONSE((short) 5, NOT_PROCESS_INDEX, RECORD_INDEX_10);

  private final short value;
  private final short processIndex;
  private final short recordIndex;

  CommandApiProcessDefinitionValueLifeCycle(
      final short value, final short processIndex, final short recordIndex) {
    this.value = value;
    this.processIndex = processIndex;
    this.recordIndex = recordIndex;
  }

  public short getValueState() {
    return value;
  }

  public static CommandApiValueLifeCycle from(final short value) {
    return switch (value) {
      case 0 -> ACTIVATE_REQUEST;
      case 1 -> ACTIVATE_RESPONSE;
      case 2 -> DELETE_REQUEST;
      case 3 -> DELETE_RESPONSE;
      case 4 -> SUSPEND_REQUEST;
      case 5 -> SUSPEND_RESPONSE;
      default -> throw new IllegalStateException("Unexpected value: " + value);
    };
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.PROCESS_DEFINITION_API;
  }

  @Override
  public short recordIndex() {
    return recordIndex;
  }
}
