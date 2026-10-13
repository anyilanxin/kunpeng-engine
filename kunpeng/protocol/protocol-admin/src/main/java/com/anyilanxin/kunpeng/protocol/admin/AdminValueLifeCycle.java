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
package com.anyilanxin.kunpeng.protocol.admin;

import static com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex.NOT_PROCESS_INDEX;

import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminClusterMetaLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessClusterMetaLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.NodeSourceLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.NodeSourceMetaLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceMetaLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.admin.AdminDispatchApiValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.business.BusinessDispatchApiValueLifeCycle;
import java.util.Arrays;
import java.util.Collection;

/**
 * 管理面协议生命周期总注册表，维护全部 LifeCycle 枚举并按 AdminValueType 路由反序列化。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface AdminValueLifeCycle {
  Collection<Class<? extends AdminValueLifeCycle>> INTENT_CLASSES =
      Arrays.asList(
          AdminDispatchPlanLifeCycle.class,
          AdminDispatchPlanExecutionLifeCycle.class,
          AdminClusterMetaLifeCycle.class,
          BusinessDispatchPlanLifeCycle.class,
          BusinessDispatchPlanExecutionLifeCycle.class,
          BusinessClusterMetaLifeCycle.class,
          AdminDispatchApiValueLifeCycle.class,
          BusinessDispatchApiValueLifeCycle.class,
          DelayedLifeCycle.class,
          NodeSourceLifeCycle.class,
          NodeSourceMetaLifeCycle.class,
          PartitionSourceLifeCycle.class,
          PartitionSourceMetaLifeCycle.class,
          UnknownState.class);
  short NULL_VAL = 255;
  AdminValueLifeCycle UNKNOWN = UnknownState.UNKNOWN;

  short value();

  String name();

  boolean isState();

  short processIndex();

  AdminValueType getValueType();

  default boolean isProcess() {
    return processIndex() != NOT_PROCESS_INDEX;
  }

  short recordIndex();

  static AdminValueLifeCycle fromProtocolValue(final AdminValueType valueType, final short state) {
    return switch (valueType) {
      case ADMIN_DISPATCH -> AdminDispatchPlanLifeCycle.from(state);
      case ADMIN_DISPATCH_EXECUTION -> AdminDispatchPlanExecutionLifeCycle.from(state);
      case ADMIN_DISPATCH_API -> AdminDispatchApiValueLifeCycle.from(state);
      case ADMIN_CLUSTER_META -> AdminClusterMetaLifeCycle.from(state);
      case BUSINESS_DISPATCH -> BusinessDispatchPlanLifeCycle.from(state);
      case BUSINESS_DISPATCH_EXECUTION -> BusinessDispatchPlanExecutionLifeCycle.from(state);
      case BUSINESS_DISPATCH_API -> BusinessDispatchApiValueLifeCycle.from(state);
      case BUSINESS_CLUSTER_META -> BusinessClusterMetaLifeCycle.from(state);
      case DELAYED -> DelayedLifeCycle.from(state);
      case NODE_SOURCE -> NodeSourceLifeCycle.from(state);
      case NODE_SOURCE_META -> NodeSourceMetaLifeCycle.from(state);
      case PARTITION_SOURCE -> PartitionSourceLifeCycle.from(state);
      case PARTITION_SOURCE_META -> PartitionSourceMetaLifeCycle.from(state);
      default -> throw new IllegalStateException("Illegal valueType: " + valueType);
    };
  }

  static int maxCardinality() {
    return INTENT_CLASSES.stream()
        .mapToInt(clazz -> clazz.getEnumConstants().length)
        .max()
        .orElse(1);
  }

  enum UnknownState implements AdminValueLifeCycle {
    UNKNOWN;

    @Override
    public short value() {
      return NULL_VAL;
    }

    @Override
    public boolean isState() {
      return false;
    }

    @Override
    public short processIndex() {
      // 无处理槽位哨兵：不能返回 0——0 是真实槽位下标，UNKNOWN 记录会误路由到首个处理器
      return NOT_PROCESS_INDEX;
    }

    @Override
    public short recordIndex() {
      return NOT_PROCESS_INDEX;
    }

    @Override
    public AdminValueType getValueType() {
      return AdminValueType.UNKNOW;
    }
  }
}
