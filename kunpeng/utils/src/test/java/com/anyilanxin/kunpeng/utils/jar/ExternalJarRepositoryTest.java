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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link ExternalJarRepository} 外部 jar 仓库测试 */
class ExternalJarRepositoryTest {

  @TempDir
  Path tempDir;

  @Test
  void rejectsNonJarExtension() throws Exception {
    try (final ExternalJarRepository repository = new ExternalJarRepository()) {
      final ExternalJarLoadException exception =
          assertThrows(
              ExternalJarLoadException.class,
              () -> repository.load(tempDir.resolve("not-jar.txt")));

      assertTrue(exception.getMessage().contains("不是 jar"));
    }
  }

  @Test
  void reportsUnreadableForMissingJar() throws Exception {
    try (final ExternalJarRepository repository = new ExternalJarRepository()) {
      final ExternalJarLoadException exception =
          assertThrows(
              ExternalJarLoadException.class,
              () -> repository.load(tempDir.resolve("missing.jar")));

      assertTrue(exception.getMessage().contains("不可读"));
    }
  }

  @Test
  void rejectsEmptyJarFile() throws Exception {
    final Path empty = Files.createFile(tempDir.resolve("empty.jar"));
    try (final ExternalJarRepository repository = new ExternalJarRepository()) {
      final ExternalJarLoadException exception =
          assertThrows(ExternalJarLoadException.class, () -> repository.load(empty));

      assertTrue(exception.getMessage().contains("文件为空"));
    }
  }

  @Test
  void loadsJarAndRegistersIt() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    try (final ExternalJarRepository repository = new ExternalJarRepository()) {
      final ExternalJarClassLoader loader = repository.load(jarFile);

      assertFalse(loader.isClosed());
      assertSame(loader, repository.getJars().get(jarFile));
      assertEquals(1, repository.getJars().size());
    }
  }

  @Test
  void reloadSamePathReusesLoader() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    try (final ExternalJarRepository repository = new ExternalJarRepository()) {
      final ExternalJarClassLoader first = repository.load(jarFile);
      final ExternalJarClassLoader second = repository.load(jarFile);

      assertSame(first, second);
      assertEquals(1, repository.getJars().size());
    }
  }

  @Test
  void sameContentDifferentPathsShareLoader() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    final Path copy = Files.copy(jarFile, tempDir.resolve("copy.jar"));
    try (final ExternalJarRepository repository = new ExternalJarRepository()) {
      final ExternalJarClassLoader first = repository.load(jarFile);
      final ExternalJarClassLoader second = repository.load(copy);

      assertSame(first, second);
      assertEquals(2, repository.getJars().size());
      assertSame(first, repository.getJars().get(copy));
    }
  }

  @Test
  void removingSharedPathKeepsLoaderOpen() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    final Path copy = Files.copy(jarFile, tempDir.resolve("copy.jar"));
    try (final ExternalJarRepository repository = new ExternalJarRepository()) {
      final ExternalJarClassLoader loader = repository.load(jarFile);
      repository.load(copy);

      final ExternalJarClassLoader removed = repository.remove(copy);

      assertSame(loader, removed);
      assertFalse(loader.isClosed(), "另一路径仍引用该加载器，不应关闭");
      assertEquals(1, repository.getJars().size());
    }
  }

  @Test
  void removingLastReferenceClosesLoader() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    try (final ExternalJarRepository repository = new ExternalJarRepository()) {
      final ExternalJarClassLoader loader = repository.load(jarFile);

      final ExternalJarClassLoader removed = repository.remove(jarFile);

      assertSame(loader, removed);
      assertTrue(loader.isClosed());
      assertTrue(repository.getJars().isEmpty());
    }
  }

  @Test
  void removingUnknownPathReturnsNull() throws Exception {
    try (final ExternalJarRepository repository = new ExternalJarRepository()) {
      assertNull(repository.remove(tempDir.resolve("unknown.jar")));
    }
  }

  @Test
  void restoresFromExistingMapAndDeduplicates() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    final Path copy = Files.copy(jarFile, tempDir.resolve("copy.jar"));
    final ExternalJarClassLoader loader = ExternalJarClassLoader.ofPath(jarFile);

    try (final ExternalJarRepository repository =
        new ExternalJarRepository(Map.of(jarFile, loader))) {
      assertSame(loader, repository.getJars().get(jarFile));

      final ExternalJarClassLoader second = repository.load(copy);
      assertSame(loader, second, "同内容 jar 应复用恢复的加载器");
    } finally {
      closeQuietly(loader);
    }
  }

  @Test
  void closingRepositoryClosesAllLoadersAndClearsSlots() throws Exception {
    final Path jarFile = TestJars.buildFixtureJar(tempDir);
    final Path copy = Files.copy(jarFile, tempDir.resolve("copy.jar"));
    final ExternalJarRepository repository = new ExternalJarRepository();
    final ExternalJarClassLoader loader = repository.load(jarFile);
    repository.load(copy);

    repository.close();

    assertTrue(loader.isClosed());
    assertTrue(repository.getJars().isEmpty());
  }

  private static void closeQuietly(final ExternalJarClassLoader loader) {
    try {
      loader.close(false);
    } catch (final IOException e) {
      // 忽略清理失败
    }
  }
}
