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
package com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventProcessors;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.processor.*;

/**
 * 流程实例相关
 *
 * @author zxuanhong
 * @since
 */
public final class AdminDispatchProcessorRegister {

  public static void registerRepository(
      final LogEventProcessors processors, final LogEventWriter writer) {
    processors
        .onCommand(new AdminDispatchChangeReplicationProcessor(writer))
        .onCommand(new AdminDispatchClusterCreatingProcessor(writer))
        .onCommand(new AdminDispatchClusterCancelProcessor(writer))
        .onCommand(new AdminDispatchClusterExecutingProcessor(writer))
        .onCommand(new AdminDispatchCompleteProcessor(writer))
        .onCommand(new AdminDispatchFailProcessor(writer));
  }
}
