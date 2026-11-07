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
package com.anyilanxin.kunpeng.client;

import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * 命令授权头挂载工具：显式入参优先于构建期注册的 provider，均在发送线程解析。 头契约与网关 {@code AuthorizationMetadataContext}
 * 同名：x-authorization-metadata（JSON）与 Authorization: Bearer。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class AuthorizationHeaders {
  private static final JsonMapper OBJECT_MAPPER = JsonMapper.builder().build();

  /** 标准 OAuth2 授权头 */
  static final Metadata.Key<String> AUTHORIZATION_HEADER =
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

  /** 授权元数据 JSON 头 */
  static final Metadata.Key<String> AUTHORIZATION_METADATA_HEADER =
      Metadata.Key.of("x-authorization-metadata", Metadata.ASCII_STRING_MARSHALLER);

  private AuthorizationHeaders() {}

  /**
   * 按需给 stub 附着授权头：显式 token/元数据优先，缺省回落到配置里的 provider；两者皆无返回原 stub。
   *
   * @param stub gRPC 异步 stub
   * @param explicitToken 命令级显式 token（可空）
   * @param explicitMetadata 命令级显式授权元数据（可空）
   * @param config 客户端配置（携带构建期注册的 provider）
   * @return 附着授权头后的 stub（无需附着时原样返回）
   */
  public static <S extends AbstractStub<S>> S attach(
      final S stub,
      final String explicitToken,
      final Map<String, Object> explicitMetadata,
      final KunpengClientConfiguration config) {
    String token = explicitToken;
    Map<String, Object> metadata = explicitMetadata;
    if (token == null && config != null && config.getAuthorizationTokenProvider() != null) {
      token = config.getAuthorizationTokenProvider().token();
    }
    if (metadata == null && config != null && config.getAuthorizationMetadataProvider() != null) {
      metadata = config.getAuthorizationMetadataProvider().authorizationMetadata();
    }
    if (token == null && (metadata == null || metadata.isEmpty())) {
      return stub;
    }
    final Metadata headers = new Metadata();
    if (metadata != null && !metadata.isEmpty()) {
      headers.put(AUTHORIZATION_METADATA_HEADER, OBJECT_MAPPER.writeValueAsString(metadata));
    }
    if (token != null) {
      headers.put(AUTHORIZATION_HEADER, "Bearer " + token);
    }
    return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
  }
}
