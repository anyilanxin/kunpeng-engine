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
package com.anyilanxin.kunpeng.gateway.grpc.service.impl;

import static com.anyilanxin.kunpeng.gateway.grpc.utils.RequestUtil.ensureJsonSet;

import com.anyilanxin.kunpeng.broker.client.business.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.business.BrokerResponse;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.job.request.JobCompleteRequest;
import com.anyilanxin.kunpeng.gateway.grpc.GrpcErrorHandle;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass;
import com.anyilanxin.kunpeng.gateway.job.GatewayJobHub;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.complete.JobCompleteResponseRecord;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayLoggers;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;

/**
 * JobService gRPC 服务：业务命令面（complete/fail/retries/timeout/throwError）经标准命令通道（commandapi）进
 * raft；消费面为流推送（OpenJobStream，注册即订阅快照）与长轮询（PullJobs，经标准激活命令全分区探测）。 完成无需独立通道——client 直接调 CompleteJob。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcJobServiceImpl extends JobServiceGrpc.JobServiceImplBase implements GrpcService {
  private static final Logger LOG = GatewayLoggers.GATEWAY_LOGGER_GRPC;

  private final BrokerClient brokerClient;
  private final GrpcErrorHandle handle;
  private final GatewayJobHub jobHub;

  public GrpcJobServiceImpl(
      final BrokerClient brokerClient, final GrpcErrorHandle handle, final GatewayJobHub jobHub) {
    this.brokerClient = brokerClient;
    this.handle = handle;
    this.jobHub = jobHub;
  }

  @Override
  public String getServiceName() {
    return "Job Service";
  }

  // —— 业务命令面 ——

  @Override
  public void updateJobRetries(
      final JobServiceOuterClass.UpdateJobRetriesRequest request,
      final StreamObserver<JobServiceOuterClass.UpdateJobRetriesResponse> responseObserver) {
    super.updateJobRetries(request, responseObserver);
  }

  @Override
  public void failJob(
      final JobServiceOuterClass.FailJobRequest request,
      final StreamObserver<JobServiceOuterClass.FailJobResponse> responseObserver) {
    super.failJob(request, responseObserver);
  }

  @Override
  public void completeJob(
      final JobServiceOuterClass.CompleteJobRequest request,
      final StreamObserver<JobServiceOuterClass.CompleteJobResponse> responseObserver) {
    final JobCompleteRequest brokerRequest =
        new JobCompleteRequest(request.getJobKey())
            .setVariables(ensureJsonSet(request.getVariables()))
            .setLocalVariables(ensureJsonSet(request.getLocalVariables()));
    final CompletableFuture<BrokerResponse<JobCompleteResponseRecord>> future =
        brokerClient.sendRequest(brokerRequest);
    future.whenComplete(
        (brokerResponse, throwable) -> {
          if (throwable != null) {
            responseObserver.onError(handle.error(responseObserver, throwable));
          } else {
            if (brokerResponse.isSuccess()) {
              final JobServiceOuterClass.CompleteJobResponse response =
                  JobServiceOuterClass.CompleteJobResponse.newBuilder().build();
              responseObserver.onNext(response);
              responseObserver.onCompleted();
            } else {
              responseObserver.onError(handle.error(responseObserver, brokerResponse));
            }
          }
        });
  }

  // —— 消费面（流推送 + 长轮询；完成走 CompleteJob 命令 rpc） ——

  @Override
  public StreamObserver<JobServiceOuterClass.OpenStreamRequest> openJobStream(
      final StreamObserver<JobServiceOuterClass.JobDelivery> responseObserver) {
    final AtomicLong streamId = new AtomicLong(-1);
    return new StreamObserver<>() {

      @Override
      public void onNext(final JobServiceOuterClass.OpenStreamRequest message) {
        switch (message.getKindCase()) {
          case REGISTER -> {
            if (streamId.get() != -1) {
              LOG.warn("Duplicate stream register ignored [stream: {}]", streamId.get());
              return;
            }
            final long id =
                jobHub.registerStream(
                    message.getRegister().getType(),
                    message.getRegister().getWorker(),
                    message.getRegister().getCapacity(),
                    responseObserver);
            streamId.set(id);
          }
          default -> LOG.debug("OpenJobStream request without kind ignored");
        }
      }

      @Override
      public void onError(final Throwable t) {
        deregister("error: " + t.getMessage());
      }

      @Override
      public void onCompleted() {
        deregister("completed");
        responseObserver.onCompleted();
      }

      private void deregister(final String cause) {
        final long id = streamId.get();
        if (id != -1) {
          jobHub.deregisterStream(id, cause);
        }
      }
    };
  }

  @Override
  public void pullJobs(
      final JobServiceOuterClass.PullRequest request,
      final StreamObserver<JobServiceOuterClass.PullResponse> responseObserver) {
    jobHub.pullJobs(request, responseObserver);
  }
}
