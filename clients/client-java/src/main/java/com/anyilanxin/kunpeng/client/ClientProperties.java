/*
 * Copyright © 2017 camunda services GmbH (info@kunpeng.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.client;

import java.net.URI;
import java.time.Duration;
import java.util.List;

public final class ClientProperties {

  /**
   * @see KunpengClientBuilder#applyEnvironmentVariableOverrides(boolean)
   */
  public static final String APPLY_ENVIRONMENT_VARIABLES_OVERRIDES =
      "kunpeng.client.applyEnvironmentVariableOverrides";

  /**
   * @see KunpengClientBuilder#grpcAddress(URI)
   */
  public static final String GRPC_ADDRESS = "kunpeng.client.gateway.grpc.address";

  /**
   * @see KunpengClientBuilder#gatewayDiscover(Duration)
   */
  public static final String GATEWAY_DISCOVER = "kunpeng.client.gateway.discover";

  /**
   * @see KunpengClientBuilder#gatewayLoadQuery(Duration)
   */
  public static final String GATEWAY_LOAD_QUERY = "kunpeng.client.gateway.loadQuery";

  /**
   * @see KunpengClientBuilder#defaultTenantId(String)
   */
  public static final String DEFAULT_TENANT_ID = "kunpeng.client.tenantId";

  /**
   * @see KunpengClientBuilder#defaultJobWorkerTenantIds(List)
   */
  public static final String DEFAULT_JOB_WORKER_TENANT_IDS = "kunpeng.client.worker.tenantIds";

  /**
   * @see KunpengClientBuilder#numJobWorkerExecutionThreads(int)
   */
  public static final String JOB_WORKER_EXECUTION_THREADS = "kunpeng.client.worker.threads";

  /**
   * @see KunpengClientBuilder#defaultJobWorkerMaxJobsActive(int)
   */
  public static final String JOB_WORKER_MAX_JOBS_ACTIVE = "kunpeng.client.worker.maxJobsActive";

  /**
   * @see KunpengClientBuilder#defaultJobWorkerName(String)
   */
  public static final String DEFAULT_JOB_WORKER_NAME = "kunpeng.client.worker.name";

  /**
   * @see KunpengClientBuilder#defaultJobTimeout(Duration)
   */
  public static final String DEFAULT_JOB_TIMEOUT = "kunpeng.client.job.timeout";

  /**
   * @see KunpengClientBuilder#defaultJobPollInterval(Duration)
   */
  public static final String DEFAULT_JOB_POLL_INTERVAL = "kunpeng.client.job.pollinterval";

  /**
   * @see KunpengClientBuilder#defaultMessageTimeToLive(Duration)
   */
  public static final String DEFAULT_MESSAGE_TIME_TO_LIVE = "kunpeng.client.message.timeToLive";

  /**
   * @see KunpengClientBuilder#defaultRequestTimeout(Duration)
   */
  public static final String DEFAULT_REQUEST_TIMEOUT = "kunpeng.client.requestTimeout";

  /**
   * @see KunpengClientBuilder#defaultRequestTimeoutOffset(Duration)
   */
  public static final String DEFAULT_REQUEST_TIMEOUT_OFFSET = "kunpeng.client.requestTimeoutOffset";

  /**
   * @see KunpengClientBuilder#usePlaintext()
   */
  public static final String USE_PLAINTEXT_CONNECTION = "kunpeng.client.security.plaintext";

  /**
   * @see KunpengClientBuilder#caCertificatePath(String)
   */
  public static final String CA_CERTIFICATE_PATH = "kunpeng.client.security.certpath";

  /**
   * @see KunpengClientBuilder#keepAlive(Duration)
   */
  public static final String KEEP_ALIVE = "kunpeng.client.keepalive";

  /**
   * @see KunpengClientBuilder#overrideAuthority(String)
   */
  public static final String OVERRIDE_AUTHORITY = "kunpeng.client.overrideauthority";

  /**
   * @see KunpengClientBuilder#maxMessageSize(int) (String)
   */
  public static final String MAX_MESSAGE_SIZE = "kunpeng.client.maxMessageSize";

  /**
   * @see KunpengClientBuilder#maxMetadataSize(int)
   */
  public static final String MAX_METADATA_SIZE = "kunpeng.client.maxMetadataSize";

  /**
   * @see KunpengClientBuilder#defaultJobWorkerStreamEnabled(boolean)
   */
  public static final String STREAM_ENABLED = "kunpeng.client.worker.stream.enabled";

  /**
   * @see KunpengClientBuilder#useDefaultRetryPolicy(boolean)
   */
  public static final String USE_DEFAULT_RETRY_POLICY = "kunpeng.client.useDefaultRetryPolicy";

  private ClientProperties() {}
}
