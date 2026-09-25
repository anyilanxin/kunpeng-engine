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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.signal;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_83;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_84;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.NOT_PROCESS_INDEX;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.PROCESS_INDEX_222;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.CommandApiValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum CommandApiSignalValueLifeCycle implements CommandApiValueLifeCycle {
  CORRELATION_REQUEST((short) 0, PROCESS_INDEX_222, RECORD_INDEX_83),
  CORRELATION_RESPONSE((short) 1, NOT_PROCESS_INDEX, RECORD_INDEX_84);

  private final short value;
  private final short processIndex;
  private final short recordIndex;

  CommandApiSignalValueLifeCycle(
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
      case 0 -> CORRELATION_REQUEST;
      case 1 -> CORRELATION_RESPONSE;
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
    return ValueType.SIGNAL_SUBSCRIPTION_API;
  }

  @Override
  public short recordIndex() {
    return recordIndex;
  }
}
