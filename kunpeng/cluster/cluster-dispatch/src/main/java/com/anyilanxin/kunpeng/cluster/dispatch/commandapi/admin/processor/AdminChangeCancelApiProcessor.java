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
package com.anyilanxin.kunpeng.cluster.dispatch.commandapi.admin.processor;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.commandapi.admin.AbstractAdminApiProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.dispatch.AdminChangeCancelRequestRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.admin.AdminDispatchApiValueLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.ImmutableRepositoryAdmin;

/**
 * @author zxuanhong
 * @since
 */
public class AdminChangeCancelApiProcessor
    extends AbstractAdminApiProcessor<AdminChangeCancelRequestRecord> {
  private final LogEventWriter writer;
  private final ImmutableRepositoryAdmin repositoryAdmin;
  private final AdminDispatchPlanRecord planRecord;

  public AdminChangeCancelApiProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryAdmin = repository.repositoryAdmin();
    planRecord = new AdminDispatchPlanRecord();
  }

  @Override
  public void processRecord(final LogRecord<AdminChangeCancelRequestRecord> record) {
    final AdminClusterMetaRecord clusterMeta = repositoryAdmin.getClusterMeta();
    if (clusterMeta == null) {
      writer.adErrorResponse(record.getRequestId(), -1, "集群未初始化，无法调度");
      return;
    }
    planRecord.reset();
    writer.addCommand(-1, AdminDispatchPlanLifeCycle.CANCELING, record.getRequestId(), planRecord);
  }

  @Override
  public AdminDispatchApiValueLifeCycle valueLifeCycle() {
    return AdminDispatchApiValueLifeCycle.CHANGE_CANCEL_REQUEST;
  }
}
