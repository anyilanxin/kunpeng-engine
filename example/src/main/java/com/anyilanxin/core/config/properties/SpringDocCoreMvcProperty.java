/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * spring doc配置
 *
 * @author zxh
 * @date 2020-08-30 15:33
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
@Component
@ConfigurationProperties(prefix = "springdoc")
public class SpringDocCoreMvcProperty {

  /** 请求头 */
  private Set<String> headers;

  /** 是否为webflux,用于spring doc打印信息路径判断使用 */
  private boolean webflux;

  /** 版本号 */
  private String version;

  /** swagger请求前缀 */
  private String apiPrefix;

  /** 联系人 */
  private String contactUser = "zxh";

  /** 联系邮箱 */
  private String contactEmail = "";

  /** 标题 */
  private String title = "";

  /** oauth2授权地址前缀 */
  private String auth2Prefix = "http://127.0.0.1/authorization";

  /** 扫包路径 */
  private String packagesToScan;

  public Set<String> getHeaders() {
    if (headers == null || headers.isEmpty()) {
      headers = new HashSet<>();
    }
    return headers;
  }
}
