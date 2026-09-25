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
package com.anyilanxin.kunpeng.utils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

/**
 * 反射工具类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ReflectUtil {

  private ReflectUtil() {}

  /**
   * 通过默认无参构造器创建指定类的新实例
   *
   * @param clazz 待实例化的类
   * @param <T> 实例的期望类型
   * @return 该类的实例
   */
  public static <T> T newInstance(final Class<T> clazz) {
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (final ReflectiveOperationException e) {
      throw new IllegalStateException(
          String.format(
              "Failed to instantiate class %s with the default constructor", clazz.getName()),
          e);
    }
  }

  /**
   * 若成员当前对给定实例对象不可访问，则将其设置为可通过反射访问。用于替代已从 JUnit 5 平台移除的 `ReflectUtils.makeAccessible`
   *
   * @param member 需要设置为可访问的成员
   * @param instance 用于检查可访问性的实例
   * @param <M> 成员类型
   * @param <U> 实例类型，通常就是 {@code Object}
   * @return 已可访问的成员
   */
  public static <M extends AccessibleObject & Member, U> M makeAccessible(
      final M member, final U instance) {
    if (!isAccessible(member, instance)) {
      member.setAccessible(true);
    }

    return member;
  }

  private static <M extends AccessibleObject & Member, U> boolean isAccessible(
      final M member, final U instance) {
    try {
      return member.canAccess(instance);
    } catch (final IllegalArgumentException e) {
      // 若 member 不是 instance 的字段或方法，canAccess 会抛 IllegalArgumentException，
      // 退化为按修饰符判断
      final var modifiers = member.getModifiers();
      return Modifier.isPublic(modifiers)
          && Modifier.isPublic(member.getDeclaringClass().getModifiers());
    }
  }

  /** 返回给定密封类的所有非接口子类型组成的流 */
  @SuppressWarnings("unchecked")
  public static <T> Stream<Class<T>> implementationsOfSealedInterface(final Class<T> clazz) {
    if (!clazz.isSealed()) {
      throw new IllegalArgumentException(String.format("Class %s is not sealed", clazz.getName()));
    }
    return concretePermittedSubtypes(clazz);
  }

  private static <T> Stream<Class<T>> concretePermittedSubtypes(final Class<T> clazz) {
    if (!clazz.isSealed()) {
      return Stream.of(clazz);
    }
    return Stream.of(clazz.getPermittedSubclasses())
        .flatMap(c -> concretePermittedSubtypes((Class<T>) c));
  }
}
