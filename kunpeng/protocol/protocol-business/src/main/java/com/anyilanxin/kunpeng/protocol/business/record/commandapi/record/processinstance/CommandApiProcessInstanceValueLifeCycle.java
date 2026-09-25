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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_15;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_16;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_17;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_18;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_19;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_20;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.CommandApiValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum CommandApiProcessInstanceValueLifeCycle implements CommandApiValueLifeCycle {
  CANCEL_REQUEST((short) 0, PROCESS_INDEX_40, RECORD_INDEX_15),
  CANCEL_RESPONSE((short) 1, NOT_PROCESS_INDEX, RECORD_INDEX_16),
  CREATE_REQUEST((short) 2, PROCESS_INDEX_41, RECORD_INDEX_17),
  CREATE_RESPONSE((short) 3, NOT_PROCESS_INDEX, RECORD_INDEX_18),
  CREATE_AND_RESULT_REQUEST((short) 4, PROCESS_INDEX_42, RECORD_INDEX_19),
  CREATE_AND_RESULT_RESPONSE((short) 5, NOT_PROCESS_INDEX, RECORD_INDEX_20);

  private final short value;
  private final short processIndex;
  private final short recordIndex;

  CommandApiProcessInstanceValueLifeCycle(
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
      case 0 -> CANCEL_REQUEST;
      case 1 -> CANCEL_RESPONSE;
      case 2 -> CREATE_REQUEST;
      case 3 -> CREATE_RESPONSE;
      case 4 -> CREATE_AND_RESULT_REQUEST;
      case 5 -> CREATE_AND_RESULT_RESPONSE;
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
    return ValueType.PROCESS_INSTANCE_API;
  }

  @Override
  public short recordIndex() {
    return recordIndex;
  }
}
