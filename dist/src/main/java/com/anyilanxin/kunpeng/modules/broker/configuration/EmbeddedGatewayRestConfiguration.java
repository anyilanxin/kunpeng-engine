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
package com.anyilanxin.kunpeng.modules.broker.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * broker 内嵌网关 REST 面装配：仅扫描 {@code gateway.rest}（REST 组件复用网关面共享 bean 与业务命令客户端）。
 *
 * <p>条件启用：{@code kunpeng.broker.gateway.enable}（缺省 true）。网关实例由 broker
 * 启动链（EmbeddedGatewayServiceStep） 创建并管理生命周期；job 中枢与拓扑视图由 modules/common 的 GatewayFaceConfiguration
 * 提供，此处不再另行装配。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    name = "kunpeng.broker.gateway.enable",
    havingValue = "true",
    matchIfMissing = true)
@Profile(value = {"broker", "restore"})
@ComponentScan(basePackages = "com.anyilanxin.kunpeng.gateway.rest")
public class EmbeddedGatewayRestConfiguration {}
