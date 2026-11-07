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

import java.util.Map;

/**
 * 授权元数据提供者：客户端构建时注册一次，每次命令发送在调用线程解析当前操作人身份并注入 {@code x-authorization-metadata} 头（标准键
 * userId/userName/roleIds）。
 *
 * <p>多用户宿主（如平台后端）可实现为读取请求级上下文（ThreadLocal）转发终端用户身份； 实现会在发送线程被调用，不做线程切换，请求级上下文可靠可见。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@FunctionalInterface
public interface AuthorizationMetadataProvider {

  /**
   * @return 当前操作人的授权元数据；null 或空 map 表示本次调用不注入身份
   */
  Map<String, Object> authorizationMetadata();
}
