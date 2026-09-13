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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.command;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordValueMapperRegister;
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

/**
 * @author zxuanhong
 * @since
 */
public class CommandRecordRegister {
  private CommandRecordRegister() {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void register(final RecordValueMapperRegister valueMapper) {
    valueMapper
        .register(AdminClusterMetaLifeCycle.NULL_VAL, AdminClusterMetaRecord::new)
        .register(
            AdminDispatchPlanExecutionLifeCycle.NULL_VAL, AdminDispatchPlanExecutionRecord::new)
        .register(AdminDispatchPlanLifeCycle.NULL_VAL, AdminDispatchPlanRecord::new)
        .register(BusinessClusterMetaLifeCycle.NULL_VAL, BusinessClusterMetaRecord::new)
        .register(
            BusinessDispatchPlanExecutionLifeCycle.NULL_VAL,
            BusinessDispatchPlanExecutionRecord::new)
        .register(BusinessDispatchPlanLifeCycle.NULL_VAL, BusinessDispatchPlanRecord::new)
        .register(DelayedLifeCycle.NULL_VAL, DelayedRecord::new)
        .register(NodeSourceLifeCycle.NULL_VAL, NodeSourceRecord::new)
        .register(NodeSourceMetaLifeCycle.NULL_VAL, NodeSourceMetaRecord::new)
        .register(PartitionSourceLifeCycle.NULL_VAL, PartitionSourceRecord::new)
        .register(PartitionSourceMetaLifeCycle.NULL_VAL, PartitionSourceMetaRecord::new);
  }
}
