/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.utils.jar;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;

/** 外部 jar 装载失败（路径 + 原因） */
public final class ExternalJarLoadException extends IOException {

  @Serial private static final long serialVersionUID = 20260818L;

  public ExternalJarLoadException(final Path jarPath, final String reason) {
    super("外部 jar 装载失败 [" + jarPath + "]: " + reason);
  }

  public ExternalJarLoadException(final Path jarPath, final String reason, final Throwable cause) {
    super("外部 jar 装载失败 [" + jarPath + "]: " + reason, cause);
  }
}
