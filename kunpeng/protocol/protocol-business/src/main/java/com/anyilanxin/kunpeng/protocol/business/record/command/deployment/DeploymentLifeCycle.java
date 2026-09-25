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

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_4;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum DeploymentLifeCycle implements ValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  CREATE((short) 0, PROCESS_INDEX_2),

  CREATED((short) 1, PROCESS_INDEX_3),

  CREATE_DISTRIBUTE((short) 2, PROCESS_INDEX_4),

  CREATED_DISTRIBUTE((short) 3, PROCESS_INDEX_5),

  CREATE_DISTRIBUTE_AFTER((short) 4, PROCESS_INDEX_6),

  DELETE((short) 5, PROCESS_INDEX_7),

  DELETED((short) 6, PROCESS_INDEX_8),

  DELETE_DISTRIBUTE((short) 7, PROCESS_INDEX_9),

  DELETED_DISTRIBUTE((short) 8, PROCESS_INDEX_10),

  DELETE_DISTRIBUTE_AFTER((short) 9, PROCESS_INDEX_11);

  private final short value;
  private final short processIndex;

  DeploymentLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public short getValueState() {
    return value;
  }

  public static ValueLifeCycle from(final short value) {
    return switch (value) {
      case 0 -> CREATE;
      case 1 -> CREATED;
      case 2 -> CREATE_DISTRIBUTE;
      case 3 -> CREATED_DISTRIBUTE;
      case 4 -> CREATE_DISTRIBUTE_AFTER;
      case 5 -> DELETE;
      case 6 -> DELETED;
      case 7 -> DELETE_DISTRIBUTE;
      case 8 -> DELETED_DISTRIBUTE;
      case 9 -> DELETE_DISTRIBUTE_AFTER;
      default -> UNKNOWN;
    };
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public boolean isEvent() {
    return true;
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.DEPLOYMENT;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_4;
  }
}
