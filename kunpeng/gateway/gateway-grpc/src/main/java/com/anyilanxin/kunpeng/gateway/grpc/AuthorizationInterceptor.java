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

import java.util.Map;

/**
 * 自定义授权拦截器 SPI（与 sink 同款外部 jar 装载：kunpeng.gateway.authorization.interceptor.className/jarPath）。
 * 挂载后由它统一承担 Bearer token → 授权元数据（标准键 userId/userName/roleIds）的转换，优先于内置 OIDC 解析。 实现类必须提供无参构造器（与 sink
 * 一致的唯一实例化入口）；转换失败直接抛异常，网关以调用错误回给客户端。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface AuthorizationInterceptor {

  /**
   * Bearer token → 授权元数据
   *
   * @param token 请求携带的 Bearer token 原文
   * @return 授权元数据 map（标准键 userId 必含）；返回 null 或空 map 表示该请求无身份
   */
  Map<String, Object> toAuthorizationMetadata(String token);
}
