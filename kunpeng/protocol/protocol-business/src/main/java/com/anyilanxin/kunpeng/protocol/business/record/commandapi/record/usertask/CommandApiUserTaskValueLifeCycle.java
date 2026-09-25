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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_32;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_33;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_34;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_35;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_36;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_37;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_38;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_39;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_40;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_41;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_42;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_43;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_44;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_45;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_46;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_47;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_48;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_49;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.CommandApiValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum CommandApiUserTaskValueLifeCycle implements CommandApiValueLifeCycle {
  ASSIGNEE_REQUEST((short) 0, PROCESS_INDEX_97, RECORD_INDEX_32),
  ASSIGNEE_RESPONSE((short) 1, NOT_PROCESS_INDEX, RECORD_INDEX_33),
  CANCEL_REQUEST((short) 2, PROCESS_INDEX_98, RECORD_INDEX_34),
  CANCEL_RESPONSE((short) 3, NOT_PROCESS_INDEX, RECORD_INDEX_35),
  CLAIM_REQUEST((short) 4, PROCESS_INDEX_99, RECORD_INDEX_36),
  CLAIM_RESPONSE((short) 5, NOT_PROCESS_INDEX, RECORD_INDEX_37),
  COMPLETE_REQUEST((short) 6, PROCESS_INDEX_100, RECORD_INDEX_38),
  COMPLETE_RESPONSE((short) 7, NOT_PROCESS_INDEX, RECORD_INDEX_39),
  CREATE_REQUEST((short) 8, PROCESS_INDEX_101, RECORD_INDEX_40),
  CREATE_RESPONSE((short) 9, NOT_PROCESS_INDEX, RECORD_INDEX_41),
  DELEGATE_REQUEST((short) 10, PROCESS_INDEX_102, RECORD_INDEX_42),
  DELEGATE_RESPONSE((short) 11, NOT_PROCESS_INDEX, RECORD_INDEX_43),
  DELETE_REQUEST((short) 12, PROCESS_INDEX_103, RECORD_INDEX_44),
  DELETE_RESPONSE((short) 13, NOT_PROCESS_INDEX, RECORD_INDEX_45),
  OWN_REQUEST((short) 14, PROCESS_INDEX_104, RECORD_INDEX_46),
  OWN_RESPONSE((short) 15, NOT_PROCESS_INDEX, RECORD_INDEX_47),
  UPDATE_REQUEST((short) 16, PROCESS_INDEX_105, RECORD_INDEX_48),
  UPDATE_RESPONSE((short) 17, NOT_PROCESS_INDEX, RECORD_INDEX_49);

  private final short value;
  private final short processIndex;
  private final short recordIndex;

  CommandApiUserTaskValueLifeCycle(
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
      case 0 -> ASSIGNEE_REQUEST;
      case 1 -> ASSIGNEE_RESPONSE;
      case 2 -> CANCEL_REQUEST;
      case 3 -> CANCEL_RESPONSE;
      case 4 -> CLAIM_REQUEST;
      case 5 -> CLAIM_RESPONSE;
      case 6 -> COMPLETE_REQUEST;
      case 7 -> COMPLETE_RESPONSE;
      case 8 -> CREATE_REQUEST;
      case 9 -> CREATE_RESPONSE;
      case 10 -> DELEGATE_REQUEST;
      case 11 -> DELEGATE_RESPONSE;
      case 12 -> DELETE_REQUEST;
      case 13 -> DELETE_RESPONSE;
      case 14 -> OWN_REQUEST;
      case 15 -> OWN_RESPONSE;
      case 16 -> UPDATE_REQUEST;
      case 17 -> UPDATE_RESPONSE;
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
    return ValueType.USER_TASK_API;
  }

  @Override
  public short recordIndex() {
    return recordIndex;
  }
}
