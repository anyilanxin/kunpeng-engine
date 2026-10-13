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
package com.anyilanxin.kunpeng.protocol.admin.record.command.business;

import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordMappingIndex.RECORD_INDEX_5;
import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandValueLifeCycle;

/**
 * 业务面集群元数据生命周期，描述集群元数据从创建、更新到删除的状态流转。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum BusinessClusterMetaLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  CREATING((short) 1, PROCESS_INDEX_42),

  CREATED((short) 2, PROCESS_INDEX_43),

  UPDATING((short) 3, PROCESS_INDEX_44),

  UPDATED((short) 4, PROCESS_INDEX_45),

  DELETING((short) 5, PROCESS_INDEX_46),

  DELETED((short) 6, PROCESS_INDEX_47);

  private final short value;
  private final short processIndex;

  BusinessClusterMetaLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public short getValueState() {
    return value;
  }

  public static AdminValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> UPDATING;
      case 4 -> UPDATED;
      case 5 -> DELETING;
      case 6 -> DELETED;
      default -> UNKNOWN;
    };
  }

  public static BusinessClusterMetaLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> CREATING;
      case 2 -> CREATED;
      case 3 -> UPDATING;
      case 4 -> UPDATED;
      case 5 -> DELETING;
      case 6 -> DELETED;
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
      case NULL_VAL, CREATING, UPDATING, DELETING -> false;
      case CREATED, UPDATED, DELETED -> true;
    };
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public AdminValueType getValueType() {
    return AdminValueType.BUSINESS_CLUSTER_META;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_5;
  }
}
