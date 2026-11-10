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

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 授权元数据拦截器：把请求头身份转换成授权元数据 Map 放进调用 {@link Context}。两路输入（元数据头优先）：
 *
 * <ol>
 *   <li>{@code x-authorization-metadata}：JSON map，直接解析；
 *   <li>{@code Authorization: Bearer <jwt>}：解码 JWT payload claims（sub→userId 标准键对齐）。
 * </ol>
 *
 * <p>Bearer token 转换优先级：自定义授权拦截器（外部 jar 挂载，kunpeng.gateway.authorization.interceptor.*）> 内置 OIDC
 * 验签解析（jwks 配置）> 免验签 claims 解码（信任边界假设上游已认证）。 网关是引擎身份的信任边界，引擎只认从这里转写进命令字段的值。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class AuthorizationMetadataInterceptor implements ServerInterceptor {
  private final AuthorizationInterceptor customInterceptor;
  private final OidcTokenResolver oidcResolver;

  /**
   * @param customInterceptor 自定义授权拦截器（token→授权元数据统一转换）；null 表示未挂载
   * @param oidcResolver 内置 OAuth2 验签解析器；null 表示未启用（Bearer 免验签解码）
   */
  public AuthorizationMetadataInterceptor(
      final AuthorizationInterceptor customInterceptor, final OidcTokenResolver oidcResolver) {
    this.customInterceptor = customInterceptor;
    this.oidcResolver = oidcResolver;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      final ServerCall<ReqT, RespT> call,
      final Metadata headers,
      final ServerCallHandler<ReqT, RespT> next) {
    final Context context =
        Context.current()
            .withValue(
                AuthorizationMetadataContext.AUTHORIZATION_METADATA, extractMetadata(headers));
    return Contexts.interceptCall(context, call, headers, next);
  }

  private Map<String, Object> extractMetadata(final Metadata headers) {
    final String metadataJson =
        headers.get(AuthorizationMetadataContext.AUTHORIZATION_METADATA_HEADER);
    if (metadataJson != null && !metadataJson.isBlank()) {
      return AuthorizationMetadataContext.parseJson(metadataJson);
    }
    final String authorization = headers.get(AuthorizationMetadataContext.AUTHORIZATION_HEADER);
    if (authorization != null
        && authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
      final String token = authorization.substring("Bearer ".length()).trim();
      if (customInterceptor != null) {
        final Map<String, Object> metadata = customInterceptor.toAuthorizationMetadata(token);
        return metadata == null ? Map.of() : metadata;
      }
      return oidcResolver == null ? jwtClaims(token) : oidcResolver.resolve(token);
    }
    return Map.of();
  }

  /** 解码 JWT payload claims（不校验签名）：格式非法直接失败，不做静默降级 */
  private static Map<String, Object> jwtClaims(final String token) {
    final int first = token.indexOf('.');
    final int last = token.lastIndexOf('.');
    if (first < 0 || last <= first) {
      throw new IllegalArgumentException("非法 Bearer token：不是 JWT 格式");
    }
    final byte[] decoded = Base64.getUrlDecoder().decode(token.substring(first + 1, last));
    final Map<String, Object> metadata =
        AuthorizationMetadataContext.parseJson(new String(decoded, StandardCharsets.UTF_8));
    AuthorizationMetadataContext.alignUserId(metadata);
    return metadata;
  }
}
