/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
 * @since
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
      return 0;
    }

    @Override
    public short recordIndex() {
      return 0;
    }

    @Override
    public AdminValueType getValueType() {
      return AdminValueType.UNKNOW;
    }
  }
}
