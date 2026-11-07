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
package com.anyilanxin.kunpeng.configuration.gateway;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 网关 OAuth2 通道配置：客户端只传 Bearer token，网关经 JWKS 验签并按 claim 映射构造授权元数据。 enabled=false 时 Bearer 走免验签的
 * claims 解码（信任上游已认证）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class GatewayAuthorizationCfg {

  /** OAuth2 token 处理开关：开启后 Bearer token 必须 JWKS 验签通过才注入身份 */
  private boolean enabled = false;

  /** JWKS 端点（如 https://auth.example.com/oauth2/jwks），enabled=true 时必填 */
  private String jwksUri;

  /** 期望的 issuer 声明（配置后校验 iss，空则不校验） */
  private String issuer;

  /** 操作人 id 的 claim 名（映射为授权元数据标准键 userId） */
  private String userIdClaim = "sub";

  /** 操作人姓名的 claim 名（空则不映射 userName） */
  private String userNameClaim;

  /** 角色 id 的 claim 名（空则不映射 roleIds） */
  private String roleIdsClaim;

  /** 自定义授权拦截器挂载（token→授权元数据统一转换，优先于内置 OIDC） */
  private InterceptorCfg interceptor = new InterceptorCfg();
}
