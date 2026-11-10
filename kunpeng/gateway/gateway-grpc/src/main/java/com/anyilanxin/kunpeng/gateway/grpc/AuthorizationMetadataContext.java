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

import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.broker.client.core.BrokerRequest;
import com.anyilanxin.kunpeng.protocol.common.UserMetadataConstant;
import com.anyilanxin.kunpeng.structpack.util.DocumentUtil;
import io.grpc.Context;
import io.grpc.Metadata;
import java.util.LinkedHashMap;
import java.util.Map;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 授权元数据上下文：{@link AuthorizationMetadataInterceptor} 把请求头的身份信息（OAuth2 Bearer token 或授权元数据
 * JSON）转换成授权元数据 Map 写入 gRPC {@link Context}，服务端实现从这里读取并整体转写进命令 Record 的编码 meta 部分。
 *
 * <p>Map 标准键见 {@link UserMetadataConstant}（userId 必填，groups/firstName/lastName/email/tenantId
 * 可选审计透传）。
 *
 * <p>头契约：{@code Authorization: Bearer <jwt>}（标准 OAuth2）或 {@code x-authorization-metadata}（JSON
 * map，二选一，元数据头优先）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class AuthorizationMetadataContext {
  /** 标准 OAuth2 授权头 */
  public static final Metadata.Key<String> AUTHORIZATION_HEADER =
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

  /** 授权元数据 JSON 请求头（非 OAuth2 场景直接携带元数据 map） */
  public static final Metadata.Key<String> AUTHORIZATION_METADATA_HEADER =
      Metadata.Key.of("x-authorization-metadata", Metadata.ASCII_STRING_MARSHALLER);

  static final Context.Key<Map<String, Object>> AUTHORIZATION_METADATA =
      Context.key("gateway-authorization-metadata");

  private AuthorizationMetadataContext() {}

  /**
   * @return 当前调用的授权元数据；调用方未携带身份时为空 Map
   */
  public static Map<String, Object> authorizationMetadata() {
    final Map<String, Object> value = AUTHORIZATION_METADATA.get();
    return value == null ? Map.of() : value;
  }

  /** 把当前调用的授权元数据转写进 broker 请求信封（统一通道，任何命令零改动复用）；元数据为空不设置。 */
  public static void attachTo(final BrokerRequest<?> request) {
    final Map<String, Object> metadata = authorizationMetadata();
    if (metadata.isEmpty()) {
      return;
    }
    final DirectBuffer encoded =
        new UnsafeBuffer(
            DocumentUtil.convertToJsonBytes(
                new UnsafeBuffer(DocumentUtil.convertToMsgPack(metadata))));
    request.setAuthorizationMetadata(encoded);
  }

  /** JSON 文本 → 授权元数据 map（可变 map，供标准键补齐） */
  static Map<String, Object> parseJson(final String json) {
    return new LinkedHashMap<>(convertToMap(new UnsafeBuffer(convertToMsgPack(json))));
  }

  /** OAuth2 sub claim 补齐 userId 标准键（已显式携带 userId 时不覆盖） */
  static void alignUserId(final Map<String, Object> metadata) {
    if (!metadata.containsKey(UserMetadataConstant.USER_ID) && metadata.containsKey("sub")) {
      metadata.put(UserMetadataConstant.USER_ID, metadata.get("sub"));
    }
  }
}
