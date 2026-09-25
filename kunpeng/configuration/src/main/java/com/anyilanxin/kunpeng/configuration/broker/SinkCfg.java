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
package com.anyilanxin.kunpeng.configuration.broker;

import java.util.Map;
import java.util.Objects;

/**
 * Sink component configuration. To be expanded eventually to allow enabling/disabling sinks, and
 * other general configuration.
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SinkCfg implements ConfigurationEntry {
  /**
   * path to the JAR file containing the sink class
   *
   * <p>optional field: if missing, will lookup the class in the broker classpath
   */
  private String jarPath;

  /** fully qualified class name pointing to the class implementing the sink SPI */
  private String className;

  /** map of arguments to use when instantiating the sink */
  private Map<String, Object> args;

  @Override
  public void init(final BrokerCfg globalConfig, final String brokerBase) {
    if (isExternal()) {
      jarPath = ConfigurationUtil.toAbsolutePath(jarPath, brokerBase);
    }
  }

  public boolean isExternal() {
    return !isEmpty(jarPath);
  }

  public String getJarPath() {
    return jarPath;
  }

  public void setJarPath(final String jarPath) {
    this.jarPath = jarPath;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(final String className) {
    this.className = className;
  }

  public Map<String, Object> getArgs() {
    return args;
  }

  public void setArgs(final Map<String, Object> args) {
    this.args = args;
  }

  private boolean isEmpty(final String value) {
    return value == null || value.isEmpty();
  }

  @Override
  public int hashCode() {
    return Objects.hash(jarPath, className, args);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final SinkCfg that = (SinkCfg) o;
    return Objects.equals(jarPath, that.jarPath)
        && Objects.equals(className, that.className)
        && Objects.equals(args, that.args);
  }

  @Override
  public String toString() {
    return "SinkCfg{"
        + ", jarPath='"
        + jarPath
        + '\''
        + ", className='"
        + className
        + '\''
        + ", args="
        + args
        + '}';
  }
}
