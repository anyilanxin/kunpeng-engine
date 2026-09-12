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
package com.anyilanxin.kunpeng.cluster.utils.serializer;

import com.anyilanxin.kunpeng.cluster.utils.Version;
import com.google.common.collect.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class Namespaces {

  /**
   * 基础类型命名空间。
   *
   * <p><b>注册顺序即 wire 契约</b>：本块处于浮动 id 段（{@code nextId(FLOATING_ID)}），Fory 按注册调用顺序分配类型 id，
   * 因此以下操作会平移后续所有类型的 id、 造成跨版本节点互解失败——<b>只允许尾部追加新类型，严禁插入、删除或重排既有条目</b>。 改动本块前先确认集群内不存在新旧版本混跑。
   */
  public static final Namespace BASIC =
      new Namespace.Builder()
          .nextId(Namespace.FLOATING_ID)
          .register(byte[].class)
          .register(AtomicBoolean.class)
          .register(AtomicInteger.class)
          .register(AtomicLong.class)
          .register(
              ImmutableList.class,
              ImmutableList.of(1).getClass(),
              ImmutableList.of(1, 2).getClass(),
              ImmutableList.of(1, 2, 3).subList(1, 3).getClass())
          .register(guavaImmutableSerializedForm())
          .register(
              ImmutableSet.class,
              ImmutableSet.of().getClass(),
              ImmutableSet.of(1).getClass(),
              ImmutableSet.of(1, 2).getClass())
          .register(
              ImmutableMap.class,
              ImmutableMap.of().getClass(),
              ImmutableMap.of("a", 1).getClass(),
              ImmutableMap.of("R", 2, "D", 2).getClass())
          .register(Collections.unmodifiableSet(Collections.emptySet()).getClass())
          .register(HashMap.class)
          .register(ConcurrentHashMap.class)
          .register(CopyOnWriteArraySet.class)
          .register(
              ArrayList.class,
              LinkedList.class,
              HashSet.class,
              LinkedHashSet.class,
              ArrayDeque.class)
          .register(HashMultiset.class)
          .register(Multisets.immutableEntry("", 0).getClass())
          .register(Maps.immutableEntry("a", "b").getClass())
          .register(Arrays.asList().getClass())
          .register(Collections.singletonList(1).getClass())
          .register(Duration.class)
          .register(Collections.emptySet().getClass())
          .register(Optional.class)
          .register(Collections.emptyList().getClass())
          .register(Collections.singleton(Object.class).getClass())
          .register(Properties.class)
          .register(int[].class)
          .register(long[].class)
          .register(short[].class)
          .register(double[].class)
          .register(float[].class)
          .register(char[].class)
          .register(String[].class)
          .register(boolean[].class)
          .register(Object[].class)
          .register(Void.class) // placeholder for the deleted LogicalTimestamp class
          .register(Void.class) // placeholder for the deleted WallClockTimestamp class
          .register(Version.class)
          .register(
              ByteBuffer.class,
              ByteBuffer.allocate(1).getClass(),
              ByteBuffer.allocateDirect(1).getClass())
          .name("BASIC")
          .build();

  /**
   * 用户自定义注册起始 id（显式绑定 Fory 类型 id 的安全下界，经 {@code nextId} 开启显式块）。Fory 预注册 JDK 常用类型占用低位段（如 ArrayList
   * 固定 id 90），显式段从 500 起避免冲突；块内按 {@code begin + index} 分配，两端按 id 对齐、与注册顺序无关。
   */
  public static final int BEGIN_USER_CUSTOM_ID = 500;

  // not to be instantiated
  private Namespaces() {}

  /**
   * Guava ImmutableList 的 subList 视图序列化时会落到包私有类 ImmutableList$SerializedForm，
   * 只能通过反射注册（ForyTypeSupportTest 已验证）。
   */
  private static Class<?> guavaImmutableSerializedForm() {
    try {
      return Class.forName("com.google.common.collect.ImmutableList$SerializedForm");
    } catch (final ClassNotFoundException e) {
      throw new IllegalStateException("Guava ImmutableList$SerializedForm not found", e);
    }
  }
}
