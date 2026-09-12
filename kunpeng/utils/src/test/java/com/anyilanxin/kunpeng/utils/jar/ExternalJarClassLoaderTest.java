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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link ExternalJarClassLoader} 隔离加载器测试 */
class ExternalJarClassLoaderTest {

  @TempDir
  Path tempDir;

  @Test
  void loadsClassFromJarAndDefinesItLocally() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    final ExternalJarClassLoader loader = ExternalJarClassLoader.ofPath(jarFile);

    final Class<?> clazz = loader.loadClass(TestJars.FIXTURE_CLASS);

    assertSame(loader, clazz.getClassLoader());
    final Method greet = clazz.getMethod("greet");
    assertEquals(TestJars.FIXTURE_GREETING, greet.invoke(null));
  }

  @Test
  void loadingSameClassTwiceReturnsSameInstance() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    final ExternalJarClassLoader loader = ExternalJarClassLoader.ofPath(jarFile);

    final Class<?> first = loader.loadClass(TestJars.FIXTURE_CLASS);
    final Class<?> second = loader.loadClass(TestJars.FIXTURE_CLASS);

    assertSame(first, second);
  }

  @Test
  void delegatesJavaClassesToSystemLoader() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    final ExternalJarClassLoader loader = ExternalJarClassLoader.ofPath(jarFile);

    assertEquals(String.class, loader.loadClass("java.lang.String"));
  }

  @Test
  void fallsBackToParentForClassesNotInJar() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    final ExternalJarClassLoader loader = ExternalJarClassLoader.ofPath(jarFile);

    final Class<?> fallback =
        loader.loadClass("com.anyilanxin.kunpeng.utils.jar.ThreadContextUtil");

    assertNotNull(fallback);
    assertNotSame(loader, fallback.getClassLoader());
  }

  @Test
  void throwsClassNotFoundWhenMissingEverywhere() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    final ExternalJarClassLoader loader = ExternalJarClassLoader.ofPath(jarFile);

    assertThrows(ClassNotFoundException.class, () -> loader.loadClass("not.exists.Missing"));
  }

  @Test
  void checksumIsHex64AndEqualForSameContent() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    final Path copy = Files.copy(jarFile, tempDir.resolve("copy.jar"));
    final Path other =
        TestJars.buildJar(
            tempDir.resolve("other"),
            "demo.Other",
            """
            package demo;

            public class Other {
              public static String greet() {
                return "other";
              }
            }
            """);

    final String checksum = ExternalJarClassLoader.ofPath(jarFile).getChecksum();
    final String sameContent = ExternalJarClassLoader.ofPath(copy).getChecksum();
    final String different = ExternalJarClassLoader.ofPath(other).getChecksum();

    assertTrue(checksum.matches("[0-9a-f]{64}"), "应为 64 位小写十六进制: " + checksum);
    assertEquals(checksum, sameContent);
    assertNotEquals(checksum, different);
  }

  @Test
  void throwsLoadExceptionForMissingPath() {
    assertThrows(
        ExternalJarLoadException.class,
        () -> ExternalJarClassLoader.ofPath(tempDir.resolve("missing.jar")));
  }

  @Test
  void closeUpdatesStateAndIsIdempotent() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    final ExternalJarClassLoader loader = ExternalJarClassLoader.ofPath(jarFile);

    assertEquals(jarFile, loader.getJarPath());
    assertTrue(!loader.isClosed());

    loader.close();
    assertTrue(loader.isClosed());

    assertDoesNotThrow(() -> loader.close(false));
  }

  @Test
  void closingFreshLoaderDoesNotThrow() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    final ExternalJarClassLoader loader = ExternalJarClassLoader.ofPath(jarFile);

    assertDoesNotThrow(() -> loader.close(false));
    assertTrue(loader.isClosed());
  }
}
