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
package com.anyilanxin.kunpeng.cluster.utils.serializer.serializers;

import java.util.Map;
import org.agrona.collections.Int2ObjectHashMap;
import org.apache.fory.context.WriteContext;
import org.apache.fory.resolver.TypeResolver;
import org.apache.fory.serializer.collection.MapSerializer;

/**
 * {@link Int2ObjectHashMap} 的 Fory 序列化器：Fory 要求 Map 类型序列化器挂载到 MapSerializer 框架 （负责 size/chunk
 * 头部读写），这里只提供条目视图与重建；agrona 内部复用迭代器/Entry，框架单次遍历 直接消费条目是安全的，读取侧经 {@code putAll} 重建实例。
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Int2ObjectHashMapSerializer extends MapSerializer<Int2ObjectHashMap> {

  /** 创建 {@link Int2ObjectHashMap} 序列化器实例。 */
  public Int2ObjectHashMapSerializer(final TypeResolver typeResolver) {
    super(typeResolver, Int2ObjectHashMap.class, true);
  }

  @Override
  public Map onMapWrite(final WriteContext writeContext, final Int2ObjectHashMap map) {
    return map;
  }

  @Override
  public Int2ObjectHashMap onMapRead(final Map map) {
    final Int2ObjectHashMap result = new Int2ObjectHashMap();
    result.putAll(map);
    return result;
  }
}
