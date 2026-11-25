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

import com.anyilanxin.kunpeng.broker.client.core.BrokerResponse;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponseCodes;
import com.anyilanxin.kunpeng.broker.client.core.LeaderChangedException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

/**
 * gRPC 错误处理：异常到 gRPC Status 的统一转换。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcErrorHandle {

  public <T> StatusRuntimeException error(
      final StreamObserver<T> streamObserver, final Throwable throwable) {
    // 解开 CompletableFuture 的 CompletionException 包装
    Throwable error = throwable;
    while (error instanceof final CompletionException completion && completion.getCause() != null) {
      error = completion.getCause();
    }
    if (error instanceof final StatusRuntimeException statusError) {
      return statusError;
    }
    if (error instanceof TimeoutException) {
      return new StatusRuntimeException(
          Status.DEADLINE_EXCEEDED.withDescription(error.getMessage()));
    }
    // 在途请求因 leader 变更被网关终止：命令可能已被执行，结果未知。与 broker 应答码 LEADER_STEPPED_DOWN 同样映射 UNAVAILABLE，
    // 网关不自动重试，是否重试由调用方决定
    if (error instanceof LeaderChangedException) {
      return new StatusRuntimeException(Status.UNAVAILABLE.withDescription(error.getMessage()));
    }
    if (error instanceof IllegalArgumentException || error instanceof NullPointerException) {
      return new StatusRuntimeException(
          Status.INVALID_ARGUMENT.withDescription(error.getMessage()));
    }
    // 其余（传输中断/引擎内部错误）如实按内部错误上报，不伪装成参数错误
    return new StatusRuntimeException(Status.INTERNAL.withDescription(error.getMessage()));
  }

  public <T> StatusRuntimeException error(
      final StreamObserver<T> streamObserver, final BrokerResponse brokerResponse) {
    // 流控/背压类拒绝（在途窗口耗尽、sink 滞后闸门）是资源不足而非业务/参数错误，映射 RESOURCE_EXHAUSTED 供调用方重试退避
    final int code = brokerResponse.code();
    if (code == BrokerResponseCodes.APPEND_REJECTED
        || code == BrokerResponseCodes.SINK_BACKPRESSURE) {
      return new StatusRuntimeException(
          Status.RESOURCE_EXHAUSTED.withDescription(brokerResponse.message()));
    }
    // 分区冻结（数据手术窗口）：暂时性不可用，计划 RESUME 后自动恢复——映射 UNAVAILABLE 供调用方重试退避
    if (code == BrokerResponseCodes.PARTITION_FROZEN) {
      return new StatusRuntimeException(
          Status.UNAVAILABLE.withDescription(brokerResponse.message()));
    }
    // leader 转移窗口拒绝（命令没进日志）与在途请求因 leader 变更被终止（结果未知）：都是暂时性不可用。
    // 前者到这里说明网关的自动重试已耗尽预算；后者网关不自动重试，是否重试由调用方决定
    if (code == BrokerResponseCodes.NOT_LEADER || code == BrokerResponseCodes.LEADER_STEPPED_DOWN) {
      return new StatusRuntimeException(
          Status.UNAVAILABLE.withDescription(brokerResponse.message()));
    }
    // broker 业务拒绝（定义不存在/状态机不符等）不是参数格式问题
    return new StatusRuntimeException(
        Status.FAILED_PRECONDITION.withDescription(brokerResponse.message()));
  }
}
