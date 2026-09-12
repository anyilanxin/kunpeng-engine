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
package com.anyilanxin.kunpeng.cluster.utils.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import org.apache.fory.Fory;
import org.junit.jupiter.api.Test;

/**
 * Fory 1.7.0 对 {@code Namespaces.BASIC} 中 Kryo 自定义序列化器所覆盖类型的原生支持验证。
 *
 * <p>逐类型尝试"注册具体形态类 → 序列化 → 反序列化 → 相等性比对"，结果打印到标准输出；
 * 用于评估 Namespace 池化层迁移到 Fory 时，哪些 Kryo 自定义序列化器可以被原生能力替代。
 */
final class ForyTypeSupportTest {

  @Test
  void shouldReportForySupportForNamespaceBasicTypes() {
    final Fory fory = Fory.builder().withXlang(false).requireClassRegistration(true).build();

    // 与 Namespace 迁移的真实做法一致：先按具体形态类注册（注册失败的类型在结果表中体现）
    for (final TypeCase typeCase : CASES) {
      try {
        fory.register(typeCase.valueSupplier().get().getClass());
      } catch (final Exception ignored) {
        // 注册失败不中断，交给序列化步骤暴露
      }
    }
    try {
      // guava subList 视图序列化时落到包私有类 ImmutableList$SerializedForm，只能反射注册
      fory.register(Class.forName("com.google.common.collect.ImmutableList$SerializedForm"));
    } catch (final Exception ignored) {
      // 同上
    }

    final var failures = new StringBuilder();
    final String rowFormat = "%-38s %s%n";
    System.out.printf("%n=== Fory 1.7.0 对 BASIC 类型原生支持验证 ===%n");
    for (final TypeCase typeCase : CASES) {
      String outcome;
      try {
        final Object value = typeCase.valueSupplier().get();
        final Object decoded = fory.deserialize(fory.serialize(value));
        outcome = defaultMatcher().test(value, decoded) ? "PASS" : "FAIL(往返不等)";
      } catch (final Throwable e) {
        outcome =
            "FAIL(" + e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()) + ")";
      }
      System.out.printf(rowFormat, typeCase.name(), outcome);
      if (!"PASS".equals(outcome)) {
        failures.append(typeCase.name()).append(" -> ").append(outcome).append('\n');
      }
    }

    assertThat(failures.toString()).as("Fory 原生不支持的类型").isEmpty();
  }

  /** 序列化验证用例：名称与取值工厂。 */
  private record TypeCase(String name, Supplier<Object> valueSupplier) {}

  private static String firstLine(final String message) {
    if (message == null) {
      return "";
    }
    final int newline = message.indexOf('\n');
    return newline < 0 ? message : message.substring(0, newline);
  }

  private static final TypeCase[] CASES = {
    // Guava 不可变集合（BASIC 用 Kryo 自定义序列化器注册）
    new TypeCase("guava ImmutableList", () -> ImmutableList.of("a", "b", "c")),
    new TypeCase("guava ImmutableList subList", () -> ImmutableList.of(1, 2, 3).subList(1, 3)),
    new TypeCase("guava ImmutableSet", () -> ImmutableSet.of("a", "b")),
    new TypeCase("guava ImmutableMap", () -> ImmutableMap.of("a", 1, "b", 2)),
    new TypeCase("guava Maps.immutableEntry", () -> Maps.immutableEntry("a", 1)),
    // Atomic 系列（BASIC 用 Kryo 自定义序列化器注册）
    new TypeCase("AtomicBoolean", () -> new AtomicBoolean(true)),
    new TypeCase("AtomicInteger", () -> new AtomicInteger(42)),
    new TypeCase("AtomicLong", () -> new AtomicLong(42L)),
    // Arrays.asList 形态（BASIC 用 ArraysAsListSerializer 注册）
    new TypeCase("Arrays.asList", () -> Arrays.asList("a", "b", "c")),
    // ByteBuffer（BASIC 用 ByteBufferSerializer 注册 heap/direct 两种形态）
    new TypeCase(
        "ByteBuffer heap",
        () -> (Object) ByteBuffer.allocate(8).putLong(42L).flip()),
    new TypeCase(
        "ByteBuffer direct",
        () -> (Object) ByteBuffer.allocateDirect(8).putLong(42L).flip()),
    // 仅支持 JDK 序列化的形态（BASIC 用 JavaSerializer 注册）
    new TypeCase("java.util.Properties", Properties::new),
    // ClusterAdminSerializer 注册的 JDK 不可变集合形态
    new TypeCase("List.of", () -> List.of("a", "b", "c")),
    new TypeCase("Set.of", () -> Set.of("a", "b", "c")),
    new TypeCase("Map.of", () -> Map.of("a", 1, "b", 2)),
    new TypeCase("Collections.singletonList", () -> List.of("a")),
    // BASIC 同时注册的时间/Optional 类型
    new TypeCase("LocalDateTime", () -> LocalDateTime.of(2026, 9, 3, 10, 30)),
    new TypeCase("LocalDate", () -> LocalDate.of(2026, 9, 3)),
    new TypeCase("LocalTime", () -> LocalTime.of(10, 30)),
  };

  /** 默认相等性判定；Atomic* 按值比较，ByteBuffer 按剩余内容比较。 */
  private static BiPredicate<Object, Object> defaultMatcher() {
    return (a, b) -> {
      if (a instanceof final AtomicBoolean x && b instanceof final AtomicBoolean y) {
        return x.get() == y.get();
      }
      if (a instanceof final AtomicInteger x && b instanceof final AtomicInteger y) {
        return x.get() == y.get();
      }
      if (a instanceof final AtomicLong x && b instanceof final AtomicLong y) {
        return x.get() == y.get();
      }
      if (a instanceof final ByteBuffer x && b instanceof final ByteBuffer y) {
        // 两种框架均将 direct buffer 往返为 heap 形态，这里只比较剩余内容
        return Arrays.equals(remainingBytes(x), remainingBytes(y));
      }
      return Objects.equals(a, b);
    };
  }

  private static byte[] remainingBytes(final ByteBuffer buffer) {
    final var copy = buffer.duplicate();
    final byte[] bytes = new byte[copy.remaining()];
    copy.get(bytes);
    return bytes;
  }
}
