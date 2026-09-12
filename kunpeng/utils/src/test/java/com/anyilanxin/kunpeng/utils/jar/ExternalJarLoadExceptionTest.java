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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** {@link ExternalJarLoadException} 单元测试 */
class ExternalJarLoadExceptionTest {

  @Test
  void messageContainsPathAndReason() {
    final ExternalJarLoadException exception =
        new ExternalJarLoadException(Path.of("/tmp/demo.jar"), "当前文件不是 jar");

    assertEquals("外部 jar 装载失败 [/tmp/demo.jar]: 当前文件不是 jar", exception.getMessage());
  }

  @Test
  void keepsUnderlyingCause() {
    final IOException cause = new IOException("io error");

    final ExternalJarLoadException exception =
        new ExternalJarLoadException(Path.of("a.jar"), "读取失败", cause);

    assertSame(cause, exception.getCause());
    assertTrue(exception.getMessage().contains("读取失败"));
  }
}
