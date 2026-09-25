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
package com.anyilanxin.kunpeng.gateway.job;

import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass.JobDelivery;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayLoggers;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

/**
 * 打开的 STREAM worker 句柄：gateway 内部数字 streamId → gRPC 响应观察者的映射载体。
 *
 * <p>同 gateway 多条同类型流并存（多 worker），broker 推送按 streamId 精确路由（workjob 设计 §3/§9.3）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class DeliveryStream {
  private static final Logger LOG = GatewayLoggers.GATEWAY_LOGGER_JOB;

  private final long streamId;
  private final String jobType;
  private final String worker;
  private final int capacity;
  private final StreamObserver<JobDelivery> observer;
  private final AtomicBoolean open = new AtomicBoolean(true);

  public DeliveryStream(
      final long streamId,
      final String jobType,
      final String worker,
      final int capacity,
      final StreamObserver<JobDelivery> observer) {
    this.streamId = streamId;
    this.jobType = jobType;
    this.worker = worker;
    this.capacity = capacity;
    this.observer = observer;
  }

  public long streamId() {
    return streamId;
  }

  public String jobType() {
    return jobType;
  }

  public String worker() {
    return worker;
  }

  public int capacity() {
    return capacity;
  }

  /** 流是否仍可写出（未关闭且 gRPC 流控就绪） */
  public boolean isWritable() {
    if (!open.get()) {
      return false;
    }
    if (observer instanceof final ServerCallStreamObserver<?> call) {
      return call.isReady();
    }
    return true;
  }

  /** 推送一条 job；返回 false 表示写不出（已关闭/流控未就绪），由调用方回报 job-push-failed */
  public boolean tryDeliver(final JobDelivery delivery) {
    if (!isWritable()) {
      return false;
    }
    try {
      observer.onNext(delivery);
      return true;
    } catch (final Exception e) {
      LOG.debug("Stream delivery failed, marking closed [stream: {}]", streamId, e);
      open.set(false);
      return false;
    }
  }

  /** 关闭句柄：幂等；返回 true 表示本次调用完成了关闭（首次） */
  public boolean close() {
    return open.compareAndSet(true, false);
  }

  public boolean isOpen() {
    return open.get();
  }
}
