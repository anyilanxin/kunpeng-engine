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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.deployment.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.deployment.DeploymentApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.deployment.delete.DeploymentDeleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.CommandApiDeploymentValueLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 部署删除 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DeploymentDeleteApiProcessor
    extends DeploymentApiAbstractProcessor<DeploymentDeleteRequestRecord> {
  private static final Logger LOG = LoggerFactory.getLogger(DeploymentDeleteApiProcessor.class);
  final LogEventWriter writer;

  public DeploymentDeleteApiProcessor(final LogEventWriter writer) {
    this.writer = writer;
  }

  @Override
  public void processRecord(final BusinessLogRecord<DeploymentDeleteRequestRecord> record) {
    LOG.debug("Received DeploymentDeleteProcessor request {}", record.getRequestId());
    writer.adResponse(
        CommandApiDeploymentValueLifeCycle.DELETE_RESPONSE,
        record.getRequestId(),
        record.getValue());
  }

  @Override
  public CommandApiDeploymentValueLifeCycle valueLifeCycle() {
    return CommandApiDeploymentValueLifeCycle.DELETE_REQUEST;
  }
}
