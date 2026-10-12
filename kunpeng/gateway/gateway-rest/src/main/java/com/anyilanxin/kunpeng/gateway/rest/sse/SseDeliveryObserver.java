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
package com.anyilanxin.kunpeng.gateway.rest.sse;

import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass.JobDelivery;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.JobVo;
import io.grpc.stub.StreamObserver;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;

/**
 * SSE 推送桥：把 {@link GatewayJobHub} 推送的 {@link JobDelivery} 逐条转成 {@code event:job} 的 SSE 事件写入响应流。
 *
 * <p>客户端断开后 onNext 抛出异常，使 {@code DeliveryStream.tryDeliver} 返回 false，broker 侧按 job-push-failed 走
 * WITHDRAW 重派（与 gRPC 流关闭语义一致）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SseDeliveryObserver implements StreamObserver<JobDelivery> {
  private final FluxSink<ServerSentEvent<JobVo>> sink;

  public SseDeliveryObserver(final FluxSink<ServerSentEvent<JobVo>> sink) {
    this.sink = sink;
  }

  @Override
  public void onNext(final JobDelivery delivery) {
    sink.next(ServerSentEvent.builder(JobVo.of(delivery)).event("job").build());
    // Reactor 取消后 next() 是静默丢弃而非抛错，必须在 next 之后补检取消态，
    // 让 DeliveryStream.tryDeliver 感知失败并走 WITHDRAW 重派
    if (sink.isCancelled()) {
      throw new IllegalStateException("SSE consumer already cancelled");
    }
  }

  @Override
  public void onError(final Throwable throwable) {
    sink.error(throwable);
  }

  @Override
  public void onCompleted() {
    sink.complete();
  }
}
