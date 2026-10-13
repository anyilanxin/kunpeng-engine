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

import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordMappingIndex.RECORD_INDEX_3;
import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex.*;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.CommandValueLifeCycle;

/**
 * 业务面调度计划生命周期，描述计划从创建、执行到完成或失败的状态流转。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum BusinessDispatchPlanLifeCycle implements CommandValueLifeCycle {
  NULL_VAL((short) -1, NOT_PROCESS_INDEX),

  CHANGE_PARTITION((short) 1, PROCESS_INDEX_22),

  CLUSTER_BALANCE((short) 2, PROCESS_INDEX_23),

  CHANGE_REPLICATION((short) 3, PROCESS_INDEX_24),

  PLAN_DELAYED((short) 4, PROCESS_INDEX_25),

  PLAN_CREATING((short) 5, PROCESS_INDEX_26),

  PLAN_CREATED((short) 6, PROCESS_INDEX_27),

  CANCELING((short) 7, PROCESS_INDEX_28),

  CANCELED((short) 8, PROCESS_INDEX_29),

  EXECUTING((short) 9, PROCESS_INDEX_30),

  EXECUTED((short) 10, PROCESS_INDEX_31),

  COMPLETING((short) 11, PROCESS_INDEX_32),

  COMPLETED((short) 12, PROCESS_INDEX_33),

  FAILING((short) 13, PROCESS_INDEX_34),

  FAILED((short) 14, PROCESS_INDEX_35);

  private final short value;
  private final short processIndex;

  BusinessDispatchPlanLifeCycle(final short value, final short processIndex) {
    this.value = value;
    this.processIndex = processIndex;
  }

  public short getValueState() {
    return value;
  }

  public static AdminValueLifeCycle from(final short value) {
    return switch (value) {
      case 1 -> CHANGE_PARTITION;
      case 2 -> CLUSTER_BALANCE;
      case 3 -> CHANGE_REPLICATION;
      case 4 -> PLAN_DELAYED;
      case 5 -> PLAN_CREATING;
      case 6 -> PLAN_CREATED;
      case 7 -> CANCELING;
      case 8 -> CANCELED;
      case 9 -> EXECUTING;
      case 10 -> EXECUTED;
      case 11 -> COMPLETING;
      case 12 -> COMPLETED;
      case 13 -> FAILING;
      case 14 -> FAILED;
      default -> UNKNOWN;
    };
  }

  public static BusinessDispatchPlanLifeCycle fromValue(final short value) {
    return switch (value) {
      case 1 -> CHANGE_PARTITION;
      case 2 -> CLUSTER_BALANCE;
      case 3 -> CHANGE_REPLICATION;
      case 4 -> PLAN_DELAYED;
      case 5 -> PLAN_CREATING;
      case 6 -> PLAN_CREATED;
      case 7 -> CANCELING;
      case 8 -> CANCELED;
      case 9 -> EXECUTING;
      case 10 -> EXECUTED;
      case 11 -> COMPLETING;
      case 12 -> COMPLETED;
      case 13 -> FAILING;
      case 14 -> FAILED;
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
      case NULL_VAL,
          CHANGE_PARTITION,
          CLUSTER_BALANCE,
          CHANGE_REPLICATION,
          PLAN_CREATING,
          CANCELING,
          EXECUTING,
          COMPLETING,
          FAILING ->
          false;
      case PLAN_DELAYED, PLAN_CREATED, CANCELED, EXECUTED, COMPLETED, FAILED -> true;
    };
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public AdminValueType getValueType() {
    return AdminValueType.BUSINESS_DISPATCH;
  }

  @Override
  public short recordIndex() {
    return RECORD_INDEX_3;
  }
}
