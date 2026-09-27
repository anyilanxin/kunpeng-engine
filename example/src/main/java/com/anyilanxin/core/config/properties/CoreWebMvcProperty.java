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
package com.anyilanxin.core.config.properties;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * app节点配置信息
 *
 * @author zxh
 * @date 2020-06-29 00:25
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
@Component
@ConfigurationProperties(prefix = "app")
public class CoreWebMvcProperty implements Serializable {
  @Serial private static final long serialVersionUID = 713575253040294540L;

  /** 运行环境 */
  @Value("${spring.profiles.active:dev}")
  private String active = "dev";

  /** 服务名称 */
  @Value("${spring.application.name:anyi}")
  private String serviceName;

  /** 当前配置文件路径 */
  @Value(value = "classpath:application-${spring.profiles.active:dev}.yml")
  private Resource resource;

  /** 请求前缀 */
  @Value("${server.servlet.context-path:/}")
  private String contentPath;

  /** 请求端口 */
  @Value("${server.port:8080}")
  private int port;

  /** 服务器ip */
  @Value("${spring.cloud.nacos.discovery.ip:}")
  private String ip;

  /** 是否生成外置配置文件 */
  private boolean createOutConf = false;

  public String getIp() {
    if (StringUtils.isBlank(ip)) {
      String machineIp = "localhost";
      try {
        machineIp = InetAddress.getLocalHost().getHostAddress();
      } catch (final UnknownHostException ignored) {
      }
      ip = machineIp;
    }
    return ip;
  }
}
