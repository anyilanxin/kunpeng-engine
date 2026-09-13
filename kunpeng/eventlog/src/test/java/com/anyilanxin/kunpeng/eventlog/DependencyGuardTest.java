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
package com.anyilanxin.kunpeng.eventlog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 依赖护栏：扫描编译产物常量池，断言无 io.camunda / netflix / guava 引用——
 * 机器保证本模块与 Zeebe 衍生构件及被替换的流控库零耦合。
 */
@DisplayName("依赖护栏: 无 io.camunda / netflix / guava")
class DependencyGuardTest {

  private static final String[] FORBIDDEN = {
      "io/camunda", "com/netflix", "com/google/common"
  };

  @Test
  void noForbiddenDependencies() throws IOException {
    final Path classesDir = Path.of("build/classes/java/main");
    assertThat(classesDir).as("编译产物存在（先跑 compileJava）").exists();

    final List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(classesDir)) {
      files.filter(p -> p.toString().endsWith(".class")).forEach(path -> {
        final byte[] bytes;
        try {
          bytes = Files.readAllBytes(path);
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
        final String binary = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        for (final String forbidden : FORBIDDEN) {
          if (binary.contains(forbidden)) {
            violations.add(path.getFileName() + " → " + forbidden);
          }
        }
      });
    }
    assertThat(violations).as("禁止引用: %s", violations).isEmpty();
  }
}
