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
package com.anyilanxin.kunpeng.sink.registry;

import com.anyilanxin.kunpeng.sink.api.RecordSink;
import com.anyilanxin.kunpeng.sink.config.MapSinkConfig;
import java.util.Map;
import java.util.Objects;

/**
 * 运行时驱动一个已配置 Sink 所需的全部信息：id、参数以及实例的创建方式。
 *
 * <p>两个描述器在 id 相同即视为相等——id 是 Sink 在集群与重启之间的身份， 而参数在两次重启之间合法地发生变化是允许的。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SinkDescriptor {

  private final SinkFactory factory;
  private final MapSinkConfig config;

  public SinkDescriptor(final String id, final Class<? extends RecordSink> sinkClass) {
    this(id, sinkClass, Map.of());
  }

  public SinkDescriptor(
      final String id,
      final Class<? extends RecordSink> sinkClass,
      final Map<String, Object> arguments) {
    this(id, new ReflectSinkFactory(id, sinkClass), arguments);
  }

  public SinkDescriptor(
      final String id, final SinkFactory factory, final Map<String, Object> arguments) {
    this.factory = Objects.requireNonNull(factory, "factory must not be null");
    this.config = new MapSinkConfig(id, arguments);
  }

  /**
   * @return 由工厂产出的全新、未配置实例
   */
  public RecordSink newInstance() throws SinkInstantiationException {
    return factory.newInstance();
  }

  /**
   * @return 本 Sink 的 id 与原始参数
   */
  public MapSinkConfig getConfig() {
    return config;
  }

  public String getId() {
    return config.getId();
  }

  /**
   * @param other 待比较的描述器
   * @return 两个描述器是否产出同一类型的 Sink
   */
  public boolean producesSameTypeAs(final SinkDescriptor other) {
    return factory.producesSameType(other.factory);
  }

  @Override
  public boolean equals(final Object other) {
    return other instanceof final SinkDescriptor that && config.getId().equals(that.config.getId());
  }

  @Override
  public int hashCode() {
    return config.getId().hashCode();
  }

  @Override
  public String toString() {
    return "SinkDescriptor{id='" + config.getId() + "'}";
  }
}
