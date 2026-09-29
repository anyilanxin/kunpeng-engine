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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fory.Fory;
import org.apache.fory.ThreadSafeFory;
import org.apache.fory.serializer.Serializer;
import org.slf4j.Logger;

/**
 * 注册块驱动的 Fory 实例容器：构建时创建线程安全的 Fory（native 模式、强制类型注册），并把全部注册块 中的类型/序列化器注册进 Fory 后再对外提供序列化能力。
 *
 * <p>非浮动注册块（{@code begin != FLOATING_ID}）内的类型按 {@code begin + index} 显式分配 Fory 类型 id 并写入线格式，
 * 两端注册顺序不同也能按 id 对齐，类型增删不会漂移其它类型的 id；浮动块内的类型仍由 Fory 按注册顺序自动分配。 构建后不允许再注册。
 */
public class Namespace {

  /** ID to use if this Namespace does not define registration id. */
  static final int FLOATING_ID = -1;

  /**
   * 显式注册起始 id：Fory 自带一批 JDK 常用类型的预注册（如 ArrayList 固定占用 id 90），低位段不可用， 与 {@link
   * Namespaces#BEGIN_USER_CUSTOM_ID} 对齐。
   */
  private static final int INITIAL_ID = Namespaces.BEGIN_USER_CUSTOM_ID;

  static final String NO_NAME = "(no name)";
  private static final Logger LOGGER = getLogger(Namespace.class);

  private final ThreadSafeFory fory;
  private final ImmutableList<RegistrationBlock> registeredBlocks;
  private final String friendlyName;

  /**
   * Creates a Fory backed namespace.
   *
   * @param registeredTypes types to register
   * @param friendlyName friendly name for the namespace
   */
  public Namespace(final List<RegistrationBlock> registeredTypes, final String friendlyName) {
    registeredBlocks = ImmutableList.copyOf(registeredTypes);
    this.friendlyName = checkNotNull(friendlyName);

    fory = Fory.builder().withXlang(false).requireClassRegistration(true).buildThreadSafeFory();
    for (final RegistrationBlock block : registeredBlocks) {
      final boolean explicitId = block.begin() != FLOATING_ID;
      int index = 0;
      for (final Pair<Class<?>[], Class<? extends Serializer<?>>> entry : block.types()) {
        final Class<? extends Serializer<?>> serializerClass = entry.getRight();
        final int id = block.begin() + index++;
        for (final Class<?> type : entry.getLeft()) {
          if (explicitId) {
            if (fory.execute(f -> f.getTypeResolver().isRegistered(type))) {
              // Fory 预注册类型（如 ArrayList 固定 id 90）或本命名空间已注册过的类型：
              // 现有 id 由 Fory 版本或首次注册固定，同样稳定，静默保留即可
              LOGGER.debug("Type {} already registered, keep existing id instead of {}", type, id);
            } else {
              try {
                fory.register(type, id);
              } catch (final IllegalArgumentException e) {
                // id 被其它类型占用属真实注册配置错误, 吞掉会把故障推迟到运行期序列化爆炸——fail-fast
                throw new IllegalStateException(
                    "Failed to register type "
                        + type.getName()
                        + " with explicit id "
                        + id
                        + " in namespace '"
                        + friendlyName
                        + "'",
                    e);
              }
            }
          } else if (serializerClass == null) {
            fory.register(type);
          }
          if (serializerClass != null) {
            // Fory 反射实例化序列化器并注入自身上下文（支持 (Config, Class)/(TypeResolver, Class) 等构造器）
            fory.registerSerializer(type, serializerClass);
          }
        }
      }
    }
  }

  /**
   * Serializes given object to byte array.
   *
   * @param obj Object to serialize
   * @return serialized bytes
   */
  public byte[] serialize(final Object obj) {
    return fory.serialize(obj);
  }

  /**
   * Deserializes given byte array to Object.
   *
   * @param bytes serialized bytes
   * @param <T> deserialized Object type
   * @return deserialized Object
   */
  public <T> T deserialize(final byte[] bytes) {
    return (T) fory.deserialize(bytes);
  }

  public ImmutableList<RegistrationBlock> getRegisteredBlocks() {
    return registeredBlocks;
  }

  @Override
  public String toString() {
    if (!friendlyName.equals(NO_NAME)) {
      return MoreObjects.toStringHelper(getClass())
          .omitNullValues()
          .add("friendlyName", friendlyName)
          // omit lengthy detail, when there's a name
          .toString();
    }
    return MoreObjects.toStringHelper(getClass())
        .add("registeredBlocks", registeredBlocks)
        .toString();
  }

  /** Namespace builder. */
  // @NotThreadSafe
  public static final class Builder {
    private int blockHeadId = INITIAL_ID;
    private List<Pair<Class<?>[], Class<? extends Serializer<?>>>> types = new ArrayList<>();
    private final List<RegistrationBlock> blocks = new ArrayList<>();
    private String name = NO_NAME;

    /**
     * Builds a {@link Namespace} instance.
     *
     * @return Namespace
     */
    public Namespace build() {
      if (!types.isEmpty()) {
        blocks.add(new RegistrationBlock(blockHeadId, types));
      }
      return new Namespace(blocks, name);
    }

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public String getName() {
      return name;
    }

    /**
     * Delimits a registration block for following register entries。块内类型按 {@code begin + index} 显式绑定
     * Fory 类型 id 并写入线格式，两端按 id 对齐、与注册顺序无关；{@link #FLOATING_ID} 表示浮动块，类型 id 仍由 Fory 自动分配。
     *
     * @param id block start id
     * @return this
     */
    public Builder nextId(final int id) {
      if (!types.isEmpty()) {
        if (id != FLOATING_ID && id < blockHeadId + types.size() && LOGGER.isWarnEnabled()) {
          LOGGER.warn(
              "requested nextId {} could potentially overlap "
                  + "with existing registrations {}+{} ",
              id,
              blockHeadId,
              types.size(),
              new RuntimeException());
        }

        blocks.add(new RegistrationBlock(blockHeadId, types));
        types = new ArrayList<>();
      }
      blockHeadId = id;
      return this;
    }

    /**
     * Registers classes to be serialized using Fory default serializer.
     *
     * @param expectedTypes list of classes
     * @return this
     */
    public Builder register(final Class<?>... expectedTypes) {
      for (final Class<?> clazz : expectedTypes) {
        types.add(Pair.of(new Class<?>[] {clazz}, null));
      }
      return this;
    }

    /**
     * Registers a Fory serializer class for the given set of classes.
     *
     * <p>传序列化器类而非实例：实例化由 Fory 完成（注入其上下文），要求序列化器提供 {@code (Config, Class)} 或 {@code (TypeResolver,
     * Class)} 构造器。
     *
     * @param classes list of classes to register
     * @param serializer serializer class to use for the classes
     * @return this
     */
    public Builder register(
        final Class<? extends Serializer<?>> serializer, final Class<?>... classes) {
      for (final Class<?> clazz : classes) {
        types.add(Pair.of(new Class[] {clazz}, checkNotNull(serializer)));
      }
      return this;
    }

    private void register(final RegistrationBlock block) {
      if (block.begin() != FLOATING_ID) {
        // flush pending types
        nextId(block.begin());
        blocks.add(block);
        nextId(block.begin() + block.types().size());
      } else {
        // 浮动块保留自动 id 语义直接并入，不重定基到显式 id 段：Fory 预注册类型的 id 由 Fory
        // 版本固定分配，重定基后会与显式段冲突
        blocks.add(block);
      }
    }

    /**
     * Registers all the class registered to given Namespace.
     *
     * @param ns Namespace
     * @return this
     */
    public Builder register(final Namespace ns) {
      if (blocks.containsAll(ns.getRegisteredBlocks())) {
        // Everything was already registered.
        LOGGER.debug("Ignoring {}, already registered.", ns);
        return this;
      }

      for (final RegistrationBlock block : ns.getRegisteredBlocks()) {
        register(block);
      }
      return this;
    }
  }

  static final class RegistrationBlock {
    private final int begin;
    private final ImmutableList<Pair<Class<?>[], Class<? extends Serializer<?>>>> types;

    RegistrationBlock(
        final int begin, final List<Pair<Class<?>[], Class<? extends Serializer<?>>>> types) {
      this.begin = begin;
      this.types = ImmutableList.copyOf(types);
    }

    public int begin() {
      return begin;
    }

    public ImmutableList<Pair<Class<?>[], Class<? extends Serializer<?>>>> types() {
      return types;
    }

    @Override
    public int hashCode() {
      return types.hashCode();
    }

    // Only the registered types are used for equality.
    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj instanceof RegistrationBlock) {
        final RegistrationBlock that = (RegistrationBlock) obj;
        return Objects.equals(types, that.types);
      }
      return false;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(getClass())
          .add("begin", begin)
          .add("types", types)
          .toString();
    }
  }
}
