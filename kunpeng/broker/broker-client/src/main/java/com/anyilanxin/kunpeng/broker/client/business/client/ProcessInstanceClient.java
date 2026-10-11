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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.client.business.client;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request.ProcessInstanceCancelRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request.ProcessInstanceCreateAndResultRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request.ProcessInstanceCreateRequest;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponse;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.cancel.ProcessInstanceCancelResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.create.ProcessInstanceCreateAndResultResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.create.ProcessInstanceCreateResponseRecord;
import java.util.concurrent.CompletableFuture;

/**
 * 流程实例域命令客户端。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ProcessInstanceClient {
  /** 创建并启动流程实例 */
  CompletableFuture<BrokerResponse<ProcessInstanceCreateResponseRecord>> createProcessInstance(
      ProcessInstanceCreateRequest request);

  /** 创建流程实例并等待结果 */
  CompletableFuture<BrokerResponse<ProcessInstanceCreateAndResultResponseRecord>>
      createProcessInstanceWithResult(ProcessInstanceCreateAndResultRequest request);

  /** 取消流程实例 */
  CompletableFuture<BrokerResponse<ProcessInstanceCancelResponseRecord>> cancelProcessInstance(
      ProcessInstanceCancelRequest request);
}
