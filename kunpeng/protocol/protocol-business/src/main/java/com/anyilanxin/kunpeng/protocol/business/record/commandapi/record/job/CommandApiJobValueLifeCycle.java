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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.*;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.CommandApiValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum CommandApiJobValueLifeCycle implements CommandApiValueLifeCycle {
  COMPLETE_REQUEST((short) 1, PROCESS_INDEX_135, RECORD_INDEX_57),
  COMPLETE_RESPONSE((short) 2, NOT_PROCESS_INDEX, RECORD_INDEX_58),
  FAIL_REQUEST((short) 3, PROCESS_INDEX_136, RECORD_INDEX_59),
  FAIL_RESPONSE((short) 4, NOT_PROCESS_INDEX, RECORD_INDEX_60);

  private final short value;
  private final short processIndex;
  private final short recordIndex;

  CommandApiJobValueLifeCycle(
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
      case 1 -> COMPLETE_REQUEST;
      case 2 -> COMPLETE_RESPONSE;
      case 3 -> FAIL_REQUEST;
      case 4 -> FAIL_RESPONSE;
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
    return ValueType.JOB_API;
  }

  @Override
  public short recordIndex() {
    return recordIndex;
  }
}
