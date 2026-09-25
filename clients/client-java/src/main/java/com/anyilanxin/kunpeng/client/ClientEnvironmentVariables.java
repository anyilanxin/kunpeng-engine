/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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

public final class ClientEnvironmentVariables {
  public static final String PLAINTEXT_CONNECTION_VAR = "KUNPENG_INSECURE_CONNECTION";
  public static final String CA_CERTIFICATE_VAR = "KUNPENG_CA_CERTIFICATE_PATH";
  public static final String KEEP_ALIVE_VAR = "KUNPENG_KEEP_ALIVE";
  public static final String OVERRIDE_AUTHORITY_VAR = "KUNPENG_OVERRIDE_AUTHORITY";
  public static final String GATEWAY_DISCOVER_VAR = "KUNPENG_GATEWAY_DISCOVER";
  public static final String GATEWAY_LOAD_QUERY_VAR = "KUNPENG_GATEWAY_LOAD_QUERY";

  public static final String KUNPENG_CLIENT_WORKER_STREAM_ENABLED =
      "KUNPENG_CLIENT_WORKER_STREAM_ENABLED";
  public static final String REST_ADDRESS_VAR = "KUNPENG_REST_ADDRESS";
  public static final String GRPC_ADDRESS_VAR = "KUNPENG_GRPC_ADDRESS";
  public static final String PREFER_REST_VAR = "KUNPENG_PREFER_REST";
  public static final String DEFAULT_TENANT_ID_VAR = "KUNPENG_DEFAULT_TENANT_ID";
  public static final String DEFAULT_JOB_WORKER_TENANT_IDS_VAR =
      "KUNPENG_DEFAULT_JOB_WORKER_TENANT_IDS";
  public static final String USE_DEFAULT_RETRY_POLICY_VAR =
      "KUNPENG_CLIENT_USE_DEFAULT_RETRY_POLICY";

  /** OAuth Environment Variables */
  public static final String OAUTH_ENV_CLIENT_ID = "KUNPENG_CLIENT_ID";

  public static final String OAUTH_ENV_CLIENT_SECRET = "KUNPENG_CLIENT_SECRET";
  public static final String OAUTH_ENV_TOKEN_AUDIENCE = "KUNPENG_TOKEN_AUDIENCE";
  public static final String OAUTH_ENV_TOKEN_SCOPE = "KUNPENG_TOKEN_SCOPE";
  public static final String OAUTH_ENV_TOKEN_RESOURCE = "KUNPENG_TOKEN_RESOURCE";
  public static final String OAUTH_ENV_AUTHORIZATION_SERVER = "KUNPENG_AUTHORIZATION_SERVER_URL";
  public static final String OAUTH_ENV_SSL_CLIENT_KEYSTORE_PATH =
      "KUNPENG_SSL_CLIENT_KEYSTORE_PATH";
  public static final String OAUTH_ENV_SSL_CLIENT_KEYSTORE_SECRET =
      "KUNPENG_SSL_CLIENT_KEYSTORE_SECRET";
  public static final String OAUTH_ENV_SSL_CLIENT_KEYSTORE_KEY_SECRET =
      "KUNPENG_SSL_CLIENT_KEYSTORE_KEY_SECRET";
  public static final String OAUTH_ENV_SSL_CLIENT_TRUSTSTORE_PATH =
      "KUNPENG_SSL_CLIENT_TRUSTSTORE_PATH";
  public static final String OAUTH_ENV_SSL_CLIENT_TRUSTSTORE_SECRET =
      "KUNPENG_SSL_CLIENT_TRUSTSTORE_SECRET";
  public static final String OAUTH_ENV_CACHE_PATH = "KUNPENG_CLIENT_CONFIG_PATH";
  public static final String OAUTH_ENV_CONNECT_TIMEOUT = "KUNPENG_AUTH_CONNECT_TIMEOUT";
  public static final String OAUTH_ENV_READ_TIMEOUT = "KUNPENG_AUTH_READ_TIMEOUT";

  private ClientEnvironmentVariables() {}
}
