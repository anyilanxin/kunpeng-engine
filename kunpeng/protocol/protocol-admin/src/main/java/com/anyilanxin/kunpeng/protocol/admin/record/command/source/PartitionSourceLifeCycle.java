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
package com.anyilanxin.kunpeng.protocol.admin.record.command.source;

import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordMappingIndex.RECORD_INDEX_25;
import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandValueLifeCycle;

/**
 * 业务面分区来源生命周期，描述单条分区来源从应用、转移到完成的状态流转。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum PartitionSourceLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  APPLYING((short) 1, PROCESS_INDEX_66),

  APPLIED((short) 2, PROCESS_INDEX_67),

  TRANSFERRED_REMOVE((short) 5, PROCESS_INDEX_70),

  TRANSFERRING_ADD((short) 3, PROCESS_INDEX_68),

  TRANSFERRED_ADD((short) 4, PROCESS_INDEX_69);

  private final short value;
  private final short processIndex;

  PartitionSourceLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public short getValueState() {
    return value;
  }

  public static AdminValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> APPLYING;
      case 2 -> APPLIED;
      case 3 -> TRANSFERRING_ADD;
      case 4 -> TRANSFERRED_ADD;
      case 5 -> TRANSFERRED_REMOVE;
      default -> UNKNOWN;
    };
  }

  public static PartitionSourceLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> APPLYING;
      case 2 -> APPLIED;
      case 3 -> TRANSFERRING_ADD;
      case 4 -> TRANSFERRED_ADD;
      case 5 -> TRANSFERRED_REMOVE;
      default -> NULL_VAL;
    };
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public boolean isState() {
    return switch (this) {
      case NULL_VAL, APPLYING, TRANSFERRING_ADD -> false;
      case APPLIED, TRANSFERRED_ADD, TRANSFERRED_REMOVE -> true;
    };
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public AdminValueType getValueType() {
    return AdminValueType.PARTITION_SOURCE;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_25;
  }
}
