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

import com.google.common.base.Preconditions;
import java.nio.file.Path;
import java.nio.file.Paths;

/** 配置工具类，提供路径解析与正整数校验等静态方法。 */
public final class ConfigurationUtil {
  public static String toAbsolutePath(final String path, final String base) {
    final Path asPath = Paths.get(path);

    if (asPath.isAbsolute()) {
      return path;
    } else {
      return Paths.get(base, path).toString();
    }
  }

  public static void checkPositive(final int value, final String configurationKey) {
    Preconditions.checkArgument(
        value > 0, "Expected %s to be > 0, but found %s", configurationKey, value);
  }
}
