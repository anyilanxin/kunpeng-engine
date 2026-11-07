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

import static com.anyilanxin.kunpeng.configuration.broker.ConfigurationUtil.toAbsolutePath;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 网关自定义授权拦截器挂载配置（形态对齐 sink：className 必填，jarPath 有值走外部 jar 装载、空则网关 classpath 查找）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class InterceptorCfg {

  /** 实现 AuthorizationInterceptor SPI 的全限定类名 */
  private String className;

  /** 外部 jar 路径（可选：缺省在网关 classpath 查找） */
  private String jarPath;

  public void init(final String basePath) {
    if (isExternal()) {
      jarPath = toAbsolutePath(jarPath, basePath);
    }
  }

  public boolean isExternal() {
    return jarPath != null && !jarPath.isBlank();
  }
}
