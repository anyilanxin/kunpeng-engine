/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.client.command.deployment;

import static com.anyilanxin.kunpeng.client.command.ArgumentUtil.ensureNotNull;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.*;
import com.anyilanxin.kunpeng.gateway.grpc.service.DeploymentServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.DeploymentServiceOuterClass;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class DeployResourceCommandImpl
    implements DeployResourceCommand, DeployResourceCommand.DeployResourceCommandStep2 {

  private static final String RESOURCES_FIELD_NAME = "resources";
  private static final String TENANT_FIELD_NAME = "tenantId";
  private final DeploymentServiceOuterClass.DeployResourceRequest.Builder requestBuilder =
      DeploymentServiceOuterClass.DeployResourceRequest.newBuilder();
  private final DeploymentServiceGrpc.DeploymentServiceStub asyncStub;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private Duration requestTimeout;
  private final JsonMapper jsonMapper;
  private String tenantId;

  public DeployResourceCommandImpl(
      final DeploymentServiceGrpc.DeploymentServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate,
      final JsonMapper jsonMapper) {
    this.asyncStub = asyncStub;
    requestTimeout = config.getDefaultRequestTimeout();
    this.retryPredicate = retryPredicate;
    tenantId(config.getDefaultTenantId());
    this.jsonMapper = jsonMapper;
    requestTimeout(requestTimeout);
  }

  @Override
  public DeployResourceCommandStep2 addResourceBytes(
      final byte[] resource, final String resourceName) {
    requestBuilder.addResources(
        DeploymentServiceOuterClass.Resource.newBuilder()
            .setName(resourceName)
            .setContent(ByteString.copyFrom(resource))
            .build());
    return this;
  }

  @Override
  public DeployResourceCommandStep2 addResourceString(
      final String resource, final Charset charset, final String resourceName) {
    return addResourceBytes(resource.getBytes(charset), resourceName);
  }

  @Override
  public DeployResourceCommandStep2 addResourceStringUtf8(
      final String resourceString, final String resourceName) {
    return addResourceString(resourceString, StandardCharsets.UTF_8, resourceName);
  }

  @Override
  public DeployResourceCommandStep2 addResourceStream(
      final InputStream resourceStream, final String resourceName) {
    ensureNotNull("resource stream", resourceStream);

    try {
      return addResourceBytes(resourceStream.readAllBytes(), resourceName);
    } catch (final IOException e) {
      final String exceptionMsg =
          String.format("Cannot deploy bpmn resource from stream. %s", e.getMessage());
      throw new ClientException(exceptionMsg, e);
    }
  }

  @Override
  public DeployResourceCommandStep2 addResourceFromClasspath(final String classpathResource) {
    ensureNotNull("classpath resource", classpathResource);

    try (final InputStream resourceStream =
        getClass().getClassLoader().getResourceAsStream(classpathResource)) {
      if (resourceStream != null) {
        return addResourceStream(resourceStream, classpathResource);
      } else {
        throw new FileNotFoundException(classpathResource);
      }

    } catch (final IOException e) {
      final String exceptionMsg =
          String.format("Cannot deploy resource from classpath. %s", e.getMessage());
      throw new RuntimeException(exceptionMsg, e);
    }
  }

  @Override
  public DeployResourceCommandStep2 addResourceFile(final String filename) {
    ensureNotNull("filename", filename);

    try (final InputStream resourceStream = new FileInputStream(filename)) {
      return addResourceStream(resourceStream, filename);
    } catch (final IOException e) {
      final String exceptionMsg =
          String.format("Cannot deploy resource from file. %s", e.getMessage());
      throw new RuntimeException(exceptionMsg, e);
    }
  }

  @Override
  public DeployResourceCommandStep2 addProcessModel(
      final BpmnModelInstance processDefinition, final String resourceName) {
    ensureNotNull("process model", processDefinition);

    final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    Bpmn.writeModelToStream(outStream, processDefinition);
    return addResourceBytes(outStream.toByteArray(), resourceName);
  }

  @Override
  public FinalCommandStep<DeployResourceCommandResponse> requestTimeout(
      final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<DeployResourceCommandResponse> send() {
    return sendGrpcRequest();
  }

  @Override
  public DeployResourceCommandStep2 tenantId(final String tenantId) {
    this.tenantId = tenantId;
    requestBuilder.setTenantId(tenantId);
    return this;
  }

  private KunpengFuture<DeployResourceCommandResponse> sendGrpcRequest() {
    final DeploymentServiceOuterClass.DeployResourceRequest request = requestBuilder.build();

    final RetriableClientFutureImpl<
            DeployResourceCommandResponse, DeploymentServiceOuterClass.DeployResourceResponse>
        future =
            new RetriableClientFutureImpl<>(
                DeployResourceCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));

    sendGrpcRequest(request, future);

    return future;
  }

  private void sendGrpcRequest(
      final DeploymentServiceOuterClass.DeployResourceRequest request,
      final StreamObserver<DeploymentServiceOuterClass.DeployResourceResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .deployResource(request, streamObserver);
  }
}
