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
package com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.processor;

import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.dispatch.AbstractAdminDispatchProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminClusterMetaLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.admin.ImmutableRepositoryAdmin;

/**
 * @author zxuanhong
 * @since
 */
public class AdminDispatchCompleteProcessor extends AbstractAdminDispatchProcessor {
  protected final LogEventWriter writer;
  private final ImmutableRepositoryAdmin repositoryAdmin;

  public AdminDispatchCompleteProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryAdmin = repository.repositoryAdmin();
  }

  @Override
  public void processRecord(final LogRecord<AdminDispatchPlanRecord> record) {
    final AdminDispatchPlanRecord value = record.getValue();
    final PartitionInfoMetaRecord meta = value.getMeta();
    // 触发调度业务节点
    final AdminClusterMetaRecord oldClusterMeta = repositoryAdmin.getClusterMeta();
    final AdminClusterMetaRecord clusterMeta =
        new AdminClusterMetaRecord()
            .setReplicationFactor(1)
            .setCurrentReplicationFactor(meta.getPartitionMembers().size())
            .setCreateTime(oldClusterMeta.getCreateTime())
            .setUpdateTime(writer.millis())
            .setMeta(meta)
            .setLastMeta(oldClusterMeta.getMeta())
            .setVersion(oldClusterMeta.getVersion() + 1);
    // 更新元数据
    writer.addCommand(AdminClusterMetaLifeCycle.UPDATING, record.getRequestId(), clusterMeta);
    // 计划标记完成（applier 复用 updateDispatchPlan 持久化）
    writer.addEvent(
        value.getDispatchPlanId(),
        AdminDispatchPlanLifeCycle.COMPLETED,
        record.getRequestId(),
        value);
  }

  @Override
  public AdminDispatchPlanLifeCycle valueLifeCycle() {
    return AdminDispatchPlanLifeCycle.COMPLETING;
  }
}
