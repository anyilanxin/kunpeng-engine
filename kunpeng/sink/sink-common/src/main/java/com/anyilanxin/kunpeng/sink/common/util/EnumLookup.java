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
package com.anyilanxin.kunpeng.sink.common.util;

import java.util.Locale;
import java.util.function.Function;

/**
 * 把外部来源的字符串映射到枚举常量的工具集。
 *
 * <p>记录负载与用户配置里的枚举经常以普通字符串出现，且写法五花八门（"IN_PROGRESS"、 "in-progress"、"InProgress"）。这里的方法会先做归一化再查找，
 * Sink 因此不必重复这类样板代码。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class EnumLookup {

  private EnumLookup() {}

  /**
   * 将 {@code name} 解析为 {@code enumClass} 的常量，忽略大小写、连字符、下划线和空格。
   *
   * @param enumClass 目标枚举
   * @param name 记录或配置中的原始值
   * @param <E> 枚举类型
   * @return 匹配的常量
   * @throws IllegalArgumentException 没有常量匹配时抛出（含 {@code null}/空白输入）
   */
  public static <E extends Enum<E>> E resolve(final Class<E> enumClass, final String name) {
    final E value = resolveOrNull(enumClass, name);
    if (value == null) {
      throw new IllegalArgumentException(
          "No constant of %s matches input '%s'".formatted(enumClass.getSimpleName(), name));
    }
    return value;
  }

  /**
   * 与 {@link #resolve(Class, String)} 相同，但没有匹配时回退到 {@code fallback} 而不是抛异常。
   *
   * @param enumClass 目标枚举
   * @param name 原始值；{@code null} 或空白时返回 {@code fallback}
   * @param fallback 无匹配时的返回值
   * @param <E> 枚举类型
   * @return 匹配的常量，或 {@code fallback}
   */
  public static <E extends Enum<E>> E resolveOrDefault(
      final Class<E> enumClass, final String name, final E fallback) {
    final E value = resolveOrNull(enumClass, name);
    return value == null ? fallback : value;
  }

  /**
   * 解析 {@code name} 后立刻用 {@code mapper} 转换；适用于记录以名称存储枚举、而目标系统需要编码的场景。
   *
   * @param enumClass 承载合法名称的枚举
   * @param name 原始值
   * @param mapper 应用到解析结果的转换函数
   * @param <E> 枚举类型
   * @param <R> 映射结果类型
   * @return 转换后的常量；无匹配时为 {@code null}
   */
  public static <E extends Enum<E>, R> R map(
      final Class<E> enumClass, final String name, final Function<E, R> mapper) {
    final E value = resolveOrNull(enumClass, name);
    return value == null ? null : mapper.apply(value);
  }

  private static <E extends Enum<E>> E resolveOrNull(final Class<E> enumClass, final String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    final String wanted = normalize(name);
    for (final E constant : enumClass.getEnumConstants()) {
      if (normalize(constant.name()).equals(wanted)) {
        return constant;
      }
    }
    return null;
  }

  private static String normalize(final String value) {
    final var builder = new StringBuilder(value.length());
    value
        .toUpperCase(Locale.ROOT)
        .chars()
        .filter(c -> Character.isLetterOrDigit(c))
        .forEach(builder::appendCodePoint);
    return builder.toString();
  }
}
