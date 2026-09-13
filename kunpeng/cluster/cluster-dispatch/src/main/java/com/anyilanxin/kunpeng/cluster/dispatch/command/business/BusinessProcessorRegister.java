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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventProcessors;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.clustermeta.BusinessClusterMetaProcessorRegister;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.BusinessDispatchProcessorRegister;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.execution.BusinessExecutionProcessorRegister;

/**
 * @author zxuanhong
 * @since
 */
public final class BusinessProcessorRegister {

  public static void registerRepository(
      final LogEventProcessors processors, final LogEventWriter writer) {
    BusinessClusterMetaProcessorRegister.registerRepository(processors, writer);
    BusinessDispatchProcessorRegister.registerRepository(processors, writer);
    BusinessExecutionProcessorRegister.registerRepository(processors, writer);
  }
}
