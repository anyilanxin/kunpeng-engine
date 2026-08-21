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
package com.anyilanxin.kunpeng.utils;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Environment {

  private static final Logger LOG = LoggerFactory.getLogger("com.anyilanxin.kunpeng.utils");

  private final Map<String, String> environment;

  public Environment() {
    this(System.getenv());
  }

  public Environment(final Map<String, String> environment) {
    this.environment = environment;
  }

  public Set<String> getPropertyKeys() {
    return Collections.unmodifiableSet(environment.keySet());
  }

  public Optional<String> get(final String name) {
    return Optional.ofNullable(environment.get(name));
  }

  public Optional<Integer> getInt(final String name) {
    try {
      return get(name).map(Integer::valueOf);
    } catch (final Exception e) {
      LOG.warn("Failed to parse environment variable {}", name, e);
      return Optional.empty();
    }
  }

  public Optional<Double> getDouble(final String name) {
    try {
      return get(name).map(Double::valueOf);
    } catch (final Exception e) {
      LOG.warn("Failed to parse environment variable {}", name, e);
      return Optional.empty();
    }
  }

  public Optional<Long> getLong(final String name) {
    try {
      return get(name).map(Long::valueOf);
    } catch (final Exception e) {
      LOG.warn("Failed to parse environment variable {}", name, e);
      return Optional.empty();
    }
  }

  public Optional<Boolean> getBool(final String name) {
    try {
      return get(name).map(Boolean::valueOf);
    } catch (final Exception e) {
      LOG.warn("Failed to parse environment variable {}", name, e);
      return Optional.empty();
    }
  }

  public Optional<List<String>> getList(final String name) {
    return get(name).map(v -> v.split(",")).map(Arrays::asList).map(StringUtil.LIST_SANITIZER);
  }
}
