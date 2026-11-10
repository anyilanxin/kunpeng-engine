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

import com.anyilanxin.kunpeng.configuration.gateway.GatewayAuthorizationCfg;
import com.anyilanxin.kunpeng.protocol.common.UserMetadataConstant;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URL;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OAuth2 token 解析器：JWKS 验签（RemoteJWKSet 自带公钥缓存与按 kid 刷新）+ 过期/issuer 校验 + claim 映射， 构造授权元数据标准键（见
 * {@link UserMetadataConstant}，名字/邮箱按 OIDC 标准 claim 默认）。验签失败直接抛异常（配置了 OAuth2 即强制验签，不做静默降级）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class OidcTokenResolver {
  private final GatewayAuthorizationCfg cfg;
  private final JWKSource<SecurityContext> jwkSource;

  private OidcTokenResolver(
      final GatewayAuthorizationCfg cfg, final JWKSource<SecurityContext> jwkSource) {
    this.cfg = cfg;
    this.jwkSource = jwkSource;
  }

  /** enabled 且 jwksUri 已配置才构造解析器；否则返回 null（Bearer 走免验签解码） */
  public static OidcTokenResolver of(final GatewayAuthorizationCfg cfg) {
    if (cfg == null || !cfg.isEnabled() || cfg.getJwksUri() == null || cfg.getJwksUri().isBlank()) {
      return null;
    }
    try {
      final DefaultResourceRetriever retriever = new DefaultResourceRetriever(5_000, 5_000);
      retriever.setConnectTimeout(5_000);
      return new OidcTokenResolver(cfg, new RemoteJWKSet(new URL(cfg.getJwksUri()), retriever));
    } catch (final java.net.MalformedURLException e) {
      throw new IllegalStateException("OAuth2 jwks-uri 配置非法:" + cfg.getJwksUri(), e);
    }
  }

  /** 验签并映射 claims → 授权元数据；token 无效/过期/issuer 不符/映射不到 userId 均抛异常 */
  public Map<String, Object> resolve(final String token) {
    final SignedJWT jwt;
    try {
      jwt = SignedJWT.parse(token);
    } catch (final java.text.ParseException e) {
      throw new IllegalArgumentException("OAuth2 token 解析失败:" + e.getMessage(), e);
    }
    final String kid = jwt.getHeader().getKeyID();
    final List<JWK> matches;
    try {
      matches = jwkSource.get(new JWKSelector(new JWKMatcher.Builder().keyID(kid).build()), null);
    } catch (final com.nimbusds.jose.KeySourceException e) {
      throw new IllegalStateException("JWKS 拉取失败:" + e.getMessage(), e);
    }
    if (matches.isEmpty()) {
      throw new IllegalArgumentException("OAuth2 token 的 kid 在 JWKS 中不存在:" + kid);
    }
    final JWTClaimsSet claims;
    try {
      claims = jwt.getJWTClaimsSet();
      if (!jwt.verify(verifier(matches.get(0)))) {
        throw new IllegalArgumentException("OAuth2 token 验签失败");
      }
    } catch (final java.text.ParseException e) {
      throw new IllegalArgumentException("OAuth2 token claims 解析失败:" + e.getMessage(), e);
    } catch (final com.nimbusds.jose.JOSEException e) {
      throw new IllegalArgumentException("OAuth2 token 验签异常:" + e.getMessage(), e);
    }
    final Date expiration = claims.getExpirationTime();
    if (expiration != null && expiration.before(new Date())) {
      throw new IllegalArgumentException("OAuth2 token 已过期");
    }
    if (cfg.getIssuer() != null
        && !cfg.getIssuer().isBlank()
        && !cfg.getIssuer().equals(claims.getIssuer())) {
      throw new IllegalArgumentException("OAuth2 token issuer 不匹配:" + claims.getIssuer());
    }
    return mapClaims(claims);
  }

  private static JWSVerifier verifier(final JWK jwk) throws com.nimbusds.jose.JOSEException {
    if (jwk instanceof final RSAKey rsaKey) {
      return new RSASSAVerifier(rsaKey.toRSAPublicKey());
    }
    if (jwk instanceof final ECKey ecKey) {
      return new ECDSAVerifier(ecKey.toECPublicKey());
    }
    throw new IllegalArgumentException("JWKS 仅支持 RSA/EC 公钥,实际:" + jwk.getKeyType());
  }

  private Map<String, Object> mapClaims(final JWTClaimsSet claims) {
    final Map<String, Object> metadata = new LinkedHashMap<>();
    try {
      final String userId = claims.getStringClaim(cfg.getUserIdClaim());
      if (userId == null || userId.isEmpty()) {
        throw new IllegalArgumentException(
            "OAuth2 token 缺少操作人 id claim(" + cfg.getUserIdClaim() + ")，无法构造授权元数据");
      }
      metadata.put(UserMetadataConstant.USER_ID, userId);
      putIfPresent(metadata, UserMetadataConstant.FIRST_NAME, cfg.getFirstNameClaim(), claims);
      putIfPresent(metadata, UserMetadataConstant.LAST_NAME, cfg.getLastNameClaim(), claims);
      putIfPresent(metadata, UserMetadataConstant.EMAIL, cfg.getEmailClaim(), claims);
      putIfPresent(metadata, UserMetadataConstant.TENANT_ID, cfg.getTenantIdClaim(), claims);
      if (cfg.getGroupsClaim() != null && !cfg.getGroupsClaim().isBlank()) {
        final List<String> groups = claims.getStringListClaim(cfg.getGroupsClaim());
        if (groups != null && !groups.isEmpty()) {
          metadata.put(UserMetadataConstant.GROUPS, groups);
        }
      }
    } catch (final java.text.ParseException e) {
      throw new IllegalArgumentException("OAuth2 token claim 类型不匹配:" + e.getMessage(), e);
    }
    return metadata;
  }

  private void putIfPresent(
      final Map<String, Object> metadata,
      final String key,
      final String claimName,
      final JWTClaimsSet claims)
      throws java.text.ParseException {
    if (claimName == null || claimName.isBlank()) {
      return;
    }
    final String value = claims.getStringClaim(claimName);
    if (value != null) {
      metadata.put(key, value);
    }
  }
}
