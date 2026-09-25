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
package com.anyilanxin.kunpeng.protocol.business.record.command.incident;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_53;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.CommandValueLifeCycle;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum IncidentLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  CREATING((short) 1, PROCESS_INDEX_127),

  CREATED((short) 2, PROCESS_INDEX_128),

  RESOLVE((short) 3, PROCESS_INDEX_129),

  RESOLVED((short) 4, PROCESS_INDEX_130),

  DELETE((short) 5, PROCESS_INDEX_131),

  DELETED((short) 6, PROCESS_INDEX_132);

  private final short value;
  private final short processIndex;

  IncidentLifeCycle(final short value, final short processIndex) {
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
    return ValueType.INCIDENT;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_53;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> RESOLVE;
      case 4 -> RESOLVED;
      case 5 -> DELETE;
      case 6 -> DELETED;
      default -> UNKNOWN;
    };
  }

  public static IncidentLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> RESOLVE;
      case 4 -> RESOLVED;
      case 5 -> DELETE;
      case 6 -> DELETED;
      default -> NULL_VAL;
    };
  }
}
