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
package com.anyilanxin.kunpeng.client.command.job;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.KunpengFuture;
import com.anyilanxin.kunpeng.client.command.RetriableStreamingFutureImpl;
import com.anyilanxin.kunpeng.client.command.job.worker.ActivatedJobImpl;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 实时流订阅命令实现（workjob 设计 §8.1）：OpenJobStream 双向流，注册即开流； job 产生后 broker 主动推达（STREAM 优先于 PULL）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class StreamJobsCommandImpl
    implements StreamJobsCommandStep1,
        StreamJobsCommandStep1.StreamJobsCommandStep2,
        StreamJobsCommandStep1.StreamJobsCommandStep3 {

  private final JobServiceGrpc.JobServiceStub asyncStub;
  private final JsonMapper jsonMapper;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final KunpengClientConfiguration config;

  private final JobServiceOuterClass.StreamRegister.Builder builder;
  private final Set<String> defaultTenantIds;
  private final Set<String> customTenantIds;

  private Consumer<ActivatedJob> consumer;
  private Duration requestTimeout;

  public StreamJobsCommandImpl(
      final JobServiceGrpc.JobServiceStub asyncStub,
      final JsonMapper jsonMapper,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate,
      final KunpengClientConfiguration config) {
    this.asyncStub = Objects.requireNonNull(asyncStub);
    this.jsonMapper = Objects.requireNonNull(jsonMapper);
    this.retryPredicate = Objects.requireNonNull(retryPredicate);
    this.config = Objects.requireNonNull(config);

    builder = JobServiceOuterClass.StreamRegister.newBuilder();
    timeout(config.getDefaultJobTimeout());
    workerName(config.getDefaultJobWorkerName());
    defaultTenantIds = new HashSet<>(config.getDefaultJobWorkerTenantIds());
    customTenantIds = new HashSet<>();
  }

  @Override
  public FinalCommandStep<StreamJobsResponse> requestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<StreamJobsResponse> send() {
    final JobServiceOuterClass.StreamRegister register = builder.build();
    final var future =
        new RetriableStreamingFutureImpl<StreamJobsResponse, JobServiceOuterClass.JobDelivery>(
            new StreamJobsResponseImpl(),
            this::consumeJob,
            retryPredicate,
            streamObserver -> issueRequest(register, streamObserver));
    issueRequest(register, future);
    return future;
  }

  /** 注册即开流：首个上行请求携带 register，随后所有下行均为推送的 JobDelivery */
  private void issueRequest(
      final JobServiceOuterClass.StreamRegister register,
      final StreamObserver<JobServiceOuterClass.JobDelivery> observer) {
    final StreamObserver<JobServiceOuterClass.OpenStreamRequest> requestObserver =
        asyncStub.openJobStream(observer);
    requestObserver.onNext(
        JobServiceOuterClass.OpenStreamRequest.newBuilder().setRegister(register).build());
  }

  private void consumeJob(final JobServiceOuterClass.JobDelivery delivery) {
    consumer.accept(new ActivatedJobImpl(jsonMapper, delivery));
  }

  @Override
  public StreamJobsCommandStep1.StreamJobsCommandStep2 jobType(final String jobType) {
    builder.setType(Objects.requireNonNull(jobType, "必须指定 job 类型"));
    return this;
  }

  @Override
  public StreamJobsCommandStep1.StreamJobsCommandStep3 consumer(
      final Consumer<ActivatedJob> consumer) {
    this.consumer = Objects.requireNonNull(consumer, "必须指定 job 消费者");
    return this;
  }

  @Override
  public StreamJobsCommandStep1.StreamJobsCommandStep3 timeout(final Duration timeout) {
    Objects.requireNonNull(timeout, "必须指定 job 超时");
    return this;
  }

  @Override
  public StreamJobsCommandStep1.StreamJobsCommandStep3 workerName(final String workerName) {
    builder.setWorker(workerName);
    return this;
  }

  @Override
  public StreamJobsCommandStep1.StreamJobsCommandStep3 fetchVariables(
      final List<String> fetchVariables) {
    // 变量裁剪由 broker 侧派发时决定；当前 wire 协议直传全量变量
    return this;
  }

  @Override
  public StreamJobsCommandStep1.StreamJobsCommandStep3 fetchVariables(
      final String... fetchVariables) {
    return fetchVariables(Arrays.asList(fetchVariables));
  }

  @Override
  public StreamJobsCommandStep1.StreamJobsCommandStep3 tenantId(final String tenantId) {
    customTenantIds.add(tenantId);
    return this;
  }

  @Override
  public StreamJobsCommandStep1.StreamJobsCommandStep3 tenantIds(final List<String> tenantIds) {
    customTenantIds.clear();
    customTenantIds.addAll(tenantIds);
    return this;
  }

  @Override
  public StreamJobsCommandStep1.StreamJobsCommandStep3 tenantIds(final String... tenantIds) {
    return tenantIds(Arrays.asList(tenantIds));
  }
}
