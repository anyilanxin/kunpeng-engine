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
package com.anyilanxin.kunpeng.cluster.dispatch.command;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventProcessors;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.AdminProcessorRegister;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.BusinessProcessorRegister;
import com.anyilanxin.kunpeng.cluster.dispatch.command.delayed.DelayedProcessorRegister;
import com.anyilanxin.kunpeng.cluster.dispatch.command.source.node.NodeSourceProcessorRegister;
import com.anyilanxin.kunpeng.cluster.dispatch.command.source.nodemeta.NodeSourceMetaProcessorRegister;
import com.anyilanxin.kunpeng.cluster.dispatch.command.source.partition.PartitionSourceProcessorRegister;
import com.anyilanxin.kunpeng.cluster.dispatch.command.source.partitionmeta.PartitionSourceMetaProcessorRegister;

/**
 * @author zxuanhong
 * @since
 */
public final class CommandProcessorRegister {

  public static void registerRepository(
      final LogEventProcessors processors, final LogEventWriter writer) {
    AdminProcessorRegister.registerRepository(processors, writer);
    BusinessProcessorRegister.registerRepository(processors, writer);
    DelayedProcessorRegister.registerRepository(processors, writer);
    NodeSourceProcessorRegister.registerRepository(processors, writer);
    NodeSourceMetaProcessorRegister.registerRepository(processors, writer);
    PartitionSourceProcessorRegister.registerRepository(processors, writer);
    PartitionSourceMetaProcessorRegister.registerRepository(processors, writer);
  }
}
