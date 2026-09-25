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

import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * broker 配置属性装配：将配置文件绑定并构建 broker 启动配置。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Component
@EnableConfigurationProperties(BrokerPropertiesConfiguration.BrokerProperties.class)
public class BrokerPropertiesConfiguration {

  @ConfigurationProperties("kunpeng.broker")
  public static class BrokerProperties extends BrokerCfg {}
}
