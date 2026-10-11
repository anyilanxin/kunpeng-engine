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
package com.anyilanxin.kunpeng.broker.client.business.client.impl;

import com.anyilanxin.kunpeng.broker.client.business.client.JobClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.job.request.JobBatchActivateRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.job.request.JobCompleteRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.job.request.JobFailRequest;
import com.anyilanxin.kunpeng.broker.client.core.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponse;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.activate.JobBatchActivateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.complete.JobCompleteResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.fail.JobFailResponseRecord;
import java.util.concurrent.CompletableFuture;

/**
 * job 域命令客户端实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DefaultJobClient implements JobClient {
  private final BrokerClient brokerClient;

  public DefaultJobClient(final BrokerClient brokerClient) {
    this.brokerClient = brokerClient;
  }

  @Override
  public CompletableFuture<BrokerResponse<JobCompleteResponseRecord>> completeJob(
      final JobCompleteRequest request) {
    return brokerClient.sendRequest(request);
  }

  @Override
  public CompletableFuture<BrokerResponse<JobFailResponseRecord>> failJob(
      final JobFailRequest request) {
    return brokerClient.sendRequest(request);
  }

  @Override
  public CompletableFuture<BrokerResponse<JobBatchActivateResponseRecord>> batchActivateJobs(
      final JobBatchActivateRequest request) {
    return brokerClient.sendRequest(request);
  }
}
