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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.broker.client.core.ApiResponseReader;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponse;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponseCodes;
import com.anyilanxin.kunpeng.broker.client.core.LeaderChangedException;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * broker 应答码与在途请求终止异常到 gRPC 状态映射单测。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class GrpcErrorHandleTest {
  private final GrpcErrorHandle handle = new GrpcErrorHandle();

  @ParameterizedTest(name = "code {0} maps to {1}")
  @CsvSource({
    "1, RESOURCE_EXHAUSTED",
    "2, RESOURCE_EXHAUSTED",
    "3, UNAVAILABLE",
    "4, UNAVAILABLE",
    "5, UNAVAILABLE",
    "99, FAILED_PRECONDITION"
  })
  @SuppressWarnings("unchecked")
  void shouldMapBrokerCodeToGrpcStatus(final int code, final Status.Code expected) {
    final StreamObserver<Void> observer = mock(StreamObserver.class);

    final StatusRuntimeException error = handle.error(observer, errorResponse(code));

    assertEquals(expected, error.getStatus().getCode());
    assertEquals("broker message " + code, error.getStatus().getDescription());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldMapARequestTerminatedByLeadershipChangeToUnavailable() {
    final StreamObserver<Void> observer = mock(StreamObserver.class);
    final PartitionId partition = PartitionId.from("raft-partition", 1);
    // 调用方拿到的是 CompletableFuture 的包装异常，处理入口收到的也是包装形态
    final Throwable wrapped =
        new CompletionException(new LeaderChangedException(partition));

    final StatusRuntimeException error = handle.error(observer, wrapped);

    assertEquals(Status.Code.UNAVAILABLE, error.getStatus().getCode());
    assertEquals(new LeaderChangedException(partition).getMessage(), error.getStatus().getDescription());
  }

  private static BrokerResponse<?> errorResponse(final int code) {
    final ApiResponseReader reader = mock(ApiResponseReader.class);
    when(reader.success()).thenReturn(false);
    when(reader.hasData()).thenReturn(false);
    when(reader.code()).thenReturn(code);
    when(reader.message()).thenReturn("broker message " + code);
    return new BrokerResponse<>(reader, null);
  }
}
