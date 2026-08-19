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

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/** 测试用 jar 夹具：运行期编译简单源码并打包为仅含单个类的 jar */
final class TestJars {

  static final String FIXTURE_CLASS = "demo.Hello";
  static final String FIXTURE_GREETING = "hello-from-jar";
  private static final String FIXTURE_SOURCE =
      """
      package demo;

      public class Hello {
        public static String greet() {
          return "%s";
        }
      }
      """.formatted(FIXTURE_GREETING);

  private TestJars() {}

  /** 编译默认夹具类并打包为 jar，返回 jar 路径 */
  static Path buildFixtureJar(final Path dir) throws IOException {
    return buildJar(dir, FIXTURE_CLASS, FIXTURE_SOURCE);
  }

  /** 编译指定源码并把产物类打包为 jar，返回 jar 路径 */
  static Path buildJar(final Path dir, final String className, final String source)
      throws IOException {
    final Path sourceFile = dir.resolve(className.replace('.', '/') + ".java");
    Files.createDirectories(sourceFile.getParent());
    Files.writeString(sourceFile, source);

    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    try (final StandardJavaFileManager fileManager =
        compiler.getStandardFileManager(null, null, null)) {
      final boolean success =
          compiler
              .getTask(
                  null,
                  fileManager,
                  null,
                  List.of("-d", dir.toString()),
                  null,
                  fileManager.getJavaFileObjects(sourceFile))
              .call();
      if (!success) {
        throw new IllegalStateException("测试夹具类编译失败: " + className);
      }
    }

    final String entryName = className.replace('.', '/') + ".class";
    final Path jarFile = dir.resolve(className.replace('.', '_') + ".jar");
    try (final JarOutputStream output = new JarOutputStream(Files.newOutputStream(jarFile))) {
      output.putNextEntry(new JarEntry(entryName));
      output.write(Files.readAllBytes(dir.resolve(entryName)));
      output.closeEntry();
    }
    return jarFile;
  }
}
