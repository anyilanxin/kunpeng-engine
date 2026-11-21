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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.modules.broker.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * broker 内嵌网关形态的 Web 控制台装配：与 {@link EmbeddedGatewayRestConfiguration} 同款条件（启用网关的 broker/restore
 * 形态）扫描 webapp 包。
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
@ComponentScan(basePackages = "com.anyilanxin.kunpeng.gateway.webapp")
public class EmbeddedGatewayWebappConfiguration {}
