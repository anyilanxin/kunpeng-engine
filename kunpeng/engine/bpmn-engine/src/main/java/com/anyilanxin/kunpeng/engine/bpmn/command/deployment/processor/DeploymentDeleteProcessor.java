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
package com.anyilanxin.kunpeng.engine.bpmn.command.deployment.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.LogEventDistributeProcessor;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DeploymentRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.DeploymentLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.CommandApiDeploymentValueLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 部署删除命令处理器：级联删除流程定义并分发。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DeploymentDeleteProcessor extends LogEventDistributeProcessor<DeploymentRecord> {
  private static final Logger LOG = LoggerFactory.getLogger(DeploymentDeleteProcessor.class);
  final LogEventWriter writer;

  public DeploymentDeleteProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
  }

  @Override
  public void processRecord(final BusinessLogRecord<DeploymentRecord> record) {
    LOG.debug("Received DeploymentDeleteProcessor request {}", record.getRequestId());
    writer.adResponse(
        CommandApiDeploymentValueLifeCycle.DELETE_RESPONSE,
        record.getRequestId(),
        record.getValue());
  }

  @Override
  public void processRecordNew(final BusinessLogRecord<DeploymentRecord> record) {}

  @Override
  public void processRecordDistribute(final BusinessLogRecord<DeploymentRecord> record) {}

  @Override
  public void processRecordDistributeAfter(final BusinessLogRecord<DeploymentRecord> record) {}

  @Override
  public ValueType valueType() {
    return ValueType.DEPLOYMENT;
  }

  @Override
  public ValueLifeCycle processRecordNewLifeCycle() {
    return DeploymentLifeCycle.DELETE;
  }

  @Override
  public ValueLifeCycle processRecordDistributeLifeCycle() {
    return DeploymentLifeCycle.DELETED_DISTRIBUTE;
  }

  @Override
  public ValueLifeCycle processRecordDistributeAfterLifeCycle() {
    return DeploymentLifeCycle.DELETE_DISTRIBUTE_AFTER;
  }
}
