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
package com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.ProcessInstanceAbstractRequest;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.create.ProcessInstanceCreateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.CommandApiProcessInstanceValueLifeCycle;

/**
 * 流程实例创建并等待结果请求。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ProcessInstanceCreateAndResultRequest
    extends ProcessInstanceAbstractRequest<ProcessInstanceCreateRequestRecord> {

  public ProcessInstanceCreateAndResultRequest() {
    super(
        CommandApiProcessInstanceValueLifeCycle.CREATE_AND_RESULT_REQUEST,
        new ProcessInstanceCreateRequestRecord());
    setPartitionId(RANDOM_PARTITION);
  }

  public ProcessInstanceCreateAndResultRequest setProcessDefinitionId(
      final long processDefinitionId) {
    getValue().setProcessDefinitionId(processDefinitionId);
    setKey(processDefinitionId);
    return this;
  }
}
