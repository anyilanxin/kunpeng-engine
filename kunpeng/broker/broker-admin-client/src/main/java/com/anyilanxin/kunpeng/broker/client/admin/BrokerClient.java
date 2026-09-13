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
package com.anyilanxin.kunpeng.broker.client.admin;

import com.anyilanxin.kunpeng.protocol.common.api.RequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.api.ResponseRecordValue;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * @author zxuanhong
 * @since
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface BrokerClient extends AutoCloseable {

  @Override
  void close();

  /**
   * 若请求指定了分区则发送至该分区，否则分配一个分区并发送。
   *
   * @param request 待发送的请求
   * @return 在收到 Broker 成功响应时完成的 future；发生错误或收到 BrokerRejection 时以异常完成。
   */
  <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      CompletableFuture<BrokerResponse<Response>> sendRequest(BrokerRequest<Request> request);

  /**
   * 若请求指定了分区则发送至该分区，否则分配一个分区并发送。请求超过指定的 requestTimeout 即超时。
   *
   * @param request 待发送的请求
   * @param requestTimeout 请求的超时时间
   * @return 在收到 Broker 成功响应时完成的 future；发生错误或收到 BrokerRejection 时以异常完成。
   */
  <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      CompletableFuture<BrokerResponse<Response>> sendRequest(
          BrokerRequest<Request> request, Duration requestTimeout);

  /**
   * 若请求指定了分区则发送至该分区，否则分配一个分区并发送。若该分区的 Leader 不可达， 则会重新发送请求，直至超时。
   *
   * @param request 待发送的请求
   * @return 在收到 Broker 成功响应时完成的 future；发生错误或收到 BrokerRejection 时以异常完成。
   */
  <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      CompletableFuture<BrokerResponse<Response>> sendRequestWithRetry(
          BrokerRequest<Request> request);

  /**
   * 若请求指定了分区则发送至该分区，否则分配一个分区并发送。若该分区的 Leader 不可达， 则会重新发送请求，直至达到给定的 requestTimeout。
   *
   * @param request 待发送的请求
   * @param requestTimeout 请求的超时时间
   * @return 在收到 Broker 成功响应时完成的 future；发生错误或收到 BrokerRejection 时以异常完成。
   */
  <Request extends RequestRecordValue, Response extends ResponseRecordValue>
      CompletableFuture<BrokerResponse<Response>> sendRequestWithRetry(
          BrokerRequest<Request> request, Duration requestTimeout);
}
