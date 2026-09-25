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

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventProcessorSingleState;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;

/**
 * 管理面调度计划命令处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class AbstractAdminDispatchProcessor
    implements LogEventProcessorSingleState<AdminDispatchPlanRecord> {
  protected final LogEventWriter writer;

  public AbstractAdminDispatchProcessor(final LogEventWriter writer) {
    this.writer = writer;
  }

  @Override
  public AdminValueType valueType() {
    return AdminValueType.ADMIN_DISPATCH;
  }

  @Override
  public abstract AdminDispatchPlanLifeCycle valueLifeCycle();
}
