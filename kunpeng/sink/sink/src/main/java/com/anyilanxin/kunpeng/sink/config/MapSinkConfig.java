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
package com.anyilanxin.kunpeng.sink.config;

import com.anyilanxin.kunpeng.sink.api.context.SinkConfiguration;
import com.anyilanxin.kunpeng.utils.ReflectUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdConvertingDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.util.StdConverter;

/**
 * 通过 broker 配置声明的 Sink 所对应的 {@link SinkConfiguration}， 其中的参数以普通 Map 的形式传入。
 *
 * <p>考虑到这些参数是手工写在 YAML/JSON 里的，绑定刻意保持宽松： 属性名与枚举值大小写不敏感、接受单引号、忽略未知属性； 数字键的 Map 会转换为
 * List，因此集合类型字段两种写法都能用。
 *
 * @param id 配置中的 Sink id
 * @param arguments 配置中的原始参数，永不为 {@code null}
 * @author zxuanhong
 * @since 2026.9.0
 */
public record MapSinkConfig(String id, Map<String, Object> arguments) implements SinkConfiguration {

  private static final ObjectMapper SETTINGS_MAPPER =
      JsonMapper.builder()
          .addModule(
              new SimpleModule().addDeserializer(List.class, new IndexedMapToListDeserializer<>()))
          .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
          .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
          .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES)
          .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();

  public MapSinkConfig {
    Objects.requireNonNull(id, "sink id must not be null");
    arguments = arguments == null ? Map.of() : arguments;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Map<String, Object> getArguments() {
    return arguments;
  }

  @Override
  public <T> T createSettings(final Class<T> settingsClass) {
    if (arguments.isEmpty()) {
      return ReflectUtil.newInstance(settingsClass);
    }
    return SETTINGS_MAPPER.convertValue(arguments, settingsClass);
  }

  /**
   * 把形如 {@code {"0": a, "2": c}} 的 Map 转成有序 List。一些配置格式没有一等公民的列表写法 （或者用户照抄了示例），会产生这种
   * Map；没有这个转换器，它们绑定到 {@code List} 字段就会失败。
   */
  static final class IndexedMapToListDeserializer<E>
      extends tools.jackson.databind.deser.std.StdConvertingDeserializer<List<E>> {

    IndexedMapToListDeserializer() {
      super(new MapToListConverter<>(null));
    }

    @Override
    public ValueDeserializer<?> createContextual(
        final DeserializationContext ctxt, final BeanProperty property) {
      // 构造一个完整接线的标准转换反序列化器：其 createContextual 会从属性解析出元素类型，
      // 并为 Map 形态创建委托反序列化器
      final var contentType = property == null ? null : property.getType().getContentType();
      return new StdConvertingDeserializer<List<E>>(new MapToListConverter<>(contentType))
          .createContextual(ctxt, property);
    }
  }

  /** 按 Map 键的数字大小排序，把值输出为 List。 */
  static final class MapToListConverter<E> extends StdConverter<Map<String, E>, List<E>> {

    private final JavaType contentType;

    MapToListConverter(final JavaType contentType) {
      this.contentType = contentType;
    }

    @Override
    public List<E> convert(final Map<String, E> value) {
      final List<Long> indices =
          value.keySet().stream().map(MapToListConverter::parseIndex).toList();
      if (indices.contains(null)) {
        throw new IllegalArgumentException(
            "Expected a map with numeric keys (list indices) but got keys %s"
                .formatted(value.keySet()));
      }

      final List<E> list = new ArrayList<>(value.size());
      value.keySet().stream()
          .sorted(java.util.Comparator.comparing(MapToListConverter::parseIndex))
          .map(value::get)
          .forEach(list::add);
      return list;
    }

    @Override
    public tools.jackson.databind.JavaType getInputType(
        final tools.jackson.databind.type.TypeFactory typeFactory) {
      final var inputType = super.getInputType(typeFactory);
      return contentType == null ? inputType : inputType.withContentType(contentType);
    }

    @Override
    public tools.jackson.databind.JavaType getOutputType(
        final tools.jackson.databind.type.TypeFactory typeFactory) {
      final var outputType = super.getOutputType(typeFactory);
      return contentType == null ? outputType : outputType.withContentType(contentType);
    }

    private static Long parseIndex(final String key) {
      try {
        return Long.parseLong(key);
      } catch (final NumberFormatException e) {
        return null;
      }
    }
  }
}
