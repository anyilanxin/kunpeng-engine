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
package com.anyilanxin.kunpeng.gateway.grpc;

import com.anyilanxin.kunpeng.broker.client.business.BrokerResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

/**
 * gRPC 错误处理：异常到 gRPC Status 的统一转换。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcErrorHandle {

  public <T> StatusRuntimeException error(
      final StreamObserver<T> streamObserver, final Throwable throwable) {
    return new StatusRuntimeException(
        Status.INVALID_ARGUMENT.withDescription(throwable.getMessage()));
  }

  public <T> StatusRuntimeException error(
      final StreamObserver<T> streamObserver, final BrokerResponse brokerResponse) {
    return new StatusRuntimeException(
        Status.INVALID_ARGUMENT.withDescription(brokerResponse.message()));
  }
}
