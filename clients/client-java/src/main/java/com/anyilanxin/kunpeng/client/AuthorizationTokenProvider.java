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

/**
 * OAuth2 token 提供者：客户端构建时注册一次，每次命令发送在调用线程解析 token 并注入 {@code Authorization: Bearer}
 * 头——token→用户信息的转换由网关处理（内置 OIDC 验签或自定义授权拦截器）。
 *
 * <p>宿主负责 token 获取（转发终端用户 token 或服务 token）；实现会在发送线程被调用。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@FunctionalInterface
public interface AuthorizationTokenProvider {

  /**
   * @return 当前调用的 Bearer token；null 表示本次调用不注入
   */
  String token();
}
