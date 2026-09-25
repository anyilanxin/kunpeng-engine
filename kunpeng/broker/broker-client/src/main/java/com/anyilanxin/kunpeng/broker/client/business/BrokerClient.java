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
package com.anyilanxin.kunpeng.broker.client.business;

import com.anyilanxin.kunpeng.cluster.config.BrokerTopologyManager;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.RequestRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.ResponseRecordValue;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * broker 业务客户端：网关向 broker 业务分区发送命令的统一入口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface BrokerClient extends AutoCloseable {

  @Override
  void close();

  /**
   * Sends a request to the partition if request specifies a partition, otherwise assign a partition
   * send it to it.
   *
   * @param request request to send
   * @return future which will be completed when a successful response from the broker is received.
   *     The future will be completed exceptionally on error or on receiving BrokerRejection.
   */
  <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      CompletableFuture<BrokerResponse<Response>> sendRequest(BrokerRequest<Request> request);

  /**
   * Sends a request to the partition if request specifies a partition, otherwise assign a partition
   * send it to it. The request times out after the specified requestTimeout.
   *
   * @param request request to send
   * @param requestTimeout timeout for the request
   * @return future which will be completed when a successful response from the broker is received.
   *     The future will be completed exceptionally on error or on receiving BrokerRejection.
   */
  <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      CompletableFuture<BrokerResponse<Response>> sendRequest(
          BrokerRequest<Request> request, Duration requestTimeout);

  /**
   * Sends a request to the partition if request specifies a partition, otherwise assign a partition
   * send it to it. If leader for that partition is not reachable the request will be resend until a
   * timeout.
   *
   * @param request request to send
   * @return future which will be completed when a successful response from the broker is
   *     received.The future will be completed exceptionally on error or on receiving
   *     BrokerRejection.
   */
  <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      CompletableFuture<BrokerResponse<Response>> sendRequestWithRetry(
          BrokerRequest<Request> request);

  /**
   * Sends a request to the partition if request specifies a partition, otherwise assign a partition
   * send it to it. If leader for that partition is not reachable the request will be resend until
   * the given requestTimeout.
   *
   * @param request request to send
   * @param requestTimeout timeout for the request
   * @return future which will be completed when a successful response from the broker is
   *     received.The future will be completed exceptionally on error or on receiving
   *     BrokerRejection.
   */
  <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      CompletableFuture<BrokerResponse<Response>> sendRequestWithRetry(
          BrokerRequest<Request> request, Duration requestTimeout);

  void subscribeJobAvailableNotification(String topic, Consumer<String> handler);

  ClusterTopologyService getTopologyService();

  BrokerTopologyManager getTopologyManager();
}
