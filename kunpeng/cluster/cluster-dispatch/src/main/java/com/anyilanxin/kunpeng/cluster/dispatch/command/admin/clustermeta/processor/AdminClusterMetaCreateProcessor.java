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
package com.anyilanxin.kunpeng.cluster.dispatch.command.admin.clustermeta.processor;

import com.anyilanxin.kunpeng.cluster.config.ClusterAdminConfiguration;
import com.anyilanxin.kunpeng.cluster.config.ClusterMetaStore;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.admin.clustermeta.AbstractAdminClusterMetaProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminClusterMetaLifeCycle;

/**
 * @author zxuanhong
 * @since
 */
public class AdminClusterMetaCreateProcessor extends AbstractAdminClusterMetaProcessor {
  protected final LogEventWriter writer;
  private final ClusterMetaStore clusterMetaStore;

  public AdminClusterMetaCreateProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    clusterMetaStore = writer.getClusterMetaStore();
  }

  @Override
  public void processRecord(final LogRecord<AdminClusterMetaRecord> record) {
    final AdminClusterMetaRecord value = record.getValue();
    writer.addEvent(-1, AdminClusterMetaLifeCycle.CREATED, record.getRequestId(), value);
    final ClusterAdminConfiguration adminConfiguration = clusterMetaStore.getAdminConfiguration();
    adminConfiguration.setVersion(1);
    adminConfiguration.setInitiator(true);
    clusterMetaStore.updateAdminConfiguration(adminConfiguration);
  }

  @Override
  public AdminClusterMetaLifeCycle valueLifeCycle() {
    return AdminClusterMetaLifeCycle.CREATING;
  }
}
