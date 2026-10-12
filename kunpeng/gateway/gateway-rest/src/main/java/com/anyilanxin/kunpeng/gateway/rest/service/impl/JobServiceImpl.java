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
package com.anyilanxin.kunpeng.gateway.rest.service.impl;

import static com.anyilanxin.kunpeng.gateway.grpc.utils.RequestUtil.ensureJsonSet;
import static com.anyilanxin.kunpeng.gateway.rest.core.BrokerCall.awaitAck;

import com.anyilanxin.kunpeng.broker.client.business.client.BusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.business.client.JobClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.job.request.JobCompleteRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.job.request.JobFailRequest;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass.PullRequest;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass.PullResponse;
import com.anyilanxin.kunpeng.gateway.job.GatewayJobHub;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.JobCompleteDto;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.JobFailDto;
import com.anyilanxin.kunpeng.gateway.rest.core.NotImplementedException;
import com.anyilanxin.kunpeng.gateway.rest.service.JobService;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.JobVo;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.PullVo;
import com.anyilanxin.kunpeng.gateway.rest.sse.SseDeliveryObserver;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * job 业务实现：命令面经标准命令通道；消费面复用 {@link GatewayJobHub}（与 gRPC 面同一注册表/长轮询表）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Service
public class JobServiceImpl implements JobService {
  private final JobClient job;
  private final GatewayJobHub jobHub;
  private final ScheduledExecutorService heartbeatScheduler;

  public JobServiceImpl(final BusinessCommandClient clients, final GatewayJobHub jobHub) {
    this.job = clients.job();
    this.jobHub = jobHub;
    this.heartbeatScheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              final Thread thread = new Thread(r, "rest-sse-heartbeat");
              thread.setDaemon(true);
              return thread;
            });
  }

  // —— 命令面 ——
  @Override
  public Mono<Void> complete(final long jobKey, final JobCompleteDto dto) {
    final JobCompleteRequest request =
        new JobCompleteRequest(jobKey)
            .setVariables(ensureJsonSet(dto.variables()))
            .setLocalVariables(ensureJsonSet(dto.localVariables()));
    return awaitAck(job.completeJob(request));
  }

  @Override
  public Mono<Void> fail(final long jobKey, final JobFailDto dto) {
    final JobFailRequest request =
        new JobFailRequest(jobKey, dto.retries(), dto.retryBackOff())
            .setErrorMessage(dto.errorMessage());
    return awaitAck(job.failJob(request));
  }

  @Override
  public Mono<Void> updateRetries(final long jobKey, final int retries) {
    return Mono.error(new NotImplementedException("updateJobRetries：协议侧尚无支撑"));
  }

  @Override
  public Mono<Void> throwError(
      final long jobKey, final String errorCode, final String errorMessage) {
    return Mono.error(new NotImplementedException("throwError：协议侧尚无支撑"));
  }

  @Override
  public Mono<Void> updateTimeout(final long jobKey, final long timeout) {
    return Mono.error(new NotImplementedException("updateJobTimeout：协议侧尚无支撑"));
  }

  // —— 消费面 ——
  @Override
  public Mono<PullVo> pull(
      final String type,
      final String worker,
      final int maxBatch,
      final long deadlineMs,
      final List<String> tenantIds) {
    return Mono.create(
        sink -> {
          final PullRequest request =
              PullRequest.newBuilder()
                  .setType(type)
                  .setWorker(worker)
                  .setMaxBatch(maxBatch)
                  .setDeadlineMs(deadlineMs)
                  .addAllTenantIds(tenantIds)
                  .build();
          final Runnable cancelPull =
              jobHub.pullJobs(
                  request,
                  new StreamObserver<>() {
                    @Override
                    public void onNext(final PullResponse response) {
                      sink.success(PullVo.of(response));
                    }

                    @Override
                    public void onError(final Throwable throwable) {
                      sink.error(throwable);
                    }

                    @Override
                    public void onCompleted() {
                      // 首个响应即整体结果，hub 不会再次回调
                    }
                  });
          // 客户端断开（SSE/HTTP 取消）立即释放挂起槽位，不留等窗口超时
          sink.onCancel(cancelPull::run);
          sink.onDispose(cancelPull::run);
        });
  }

  @Override
  public Flux<ServerSentEvent<JobVo>> stream(
      final String jobType,
      final String worker,
      final int capacity,
      final List<String> tenantIds,
      final List<String> fetchVariables,
      final long heartbeatMs) {
    return Flux.create(
        sink -> {
          final SseDeliveryObserver observer = new SseDeliveryObserver(sink);
          final long streamId =
              jobHub.registerStream(jobType, worker, capacity, tenantIds, fetchVariables, observer);
          final ScheduledFuture<?> heartbeat =
              heartbeatScheduler.scheduleAtFixedRate(
                  () -> sink.next(keepAlive()), heartbeatMs, heartbeatMs, TimeUnit.MILLISECONDS);
          sink.onDispose(
              () -> {
                heartbeat.cancel(false);
                jobHub.deregisterStream(streamId, "rest client disconnected");
              });
        });
  }

  private static ServerSentEvent<JobVo> keepAlive() {
    return ServerSentEvent.<JobVo>builder().comment("keep-alive").build();
  }

  @PreDestroy
  void shutdown() {
    heartbeatScheduler.shutdownNow();
  }
}
