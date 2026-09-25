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

package com.anyilanxin.kunpeng.bpm.parse.dmn.type.impl;

import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnDataTypeTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnDataTypeTransformerRegistry;
import java.util.HashMap;
import java.util.Map;

/**
 * 面向内置 {@link DmnDataTypeTransformer} 的 {@link DmnDataTypeTransformerRegistry} 实现。
 *
 * @author Philipp Ossler
 * @since 2026.9.0
 */
public class DefaultDataTypeTransformerRegistry implements DmnDataTypeTransformerRegistry {

  protected static final Map<String, DmnDataTypeTransformer> transformers =
      getDefaultTransformers();

  protected static Map<String, DmnDataTypeTransformer> getDefaultTransformers() {
    final Map<String, DmnDataTypeTransformer> transformers = new HashMap<>();

    transformers.put("string", new StringDataTypeTransformer());
    transformers.put("boolean", new BooleanDataTypeTransformer());
    transformers.put("integer", new IntegerDataTypeTransformer());
    transformers.put("long", new LongDataTypeTransformer());
    transformers.put("double", new DoubleDataTypeTransformer());
    transformers.put("date", new DateDataTypeTransformer());

    return transformers;
  }

  @Override
  public void addTransformer(final String typeName, final DmnDataTypeTransformer transformer) {
    transformers.put(typeName, transformer);
  }

  @Override
  public DmnDataTypeTransformer getTransformer(final String typeName) {
    if (typeName != null && transformers.containsKey(typeName.toLowerCase())) {
      return transformers.get(typeName.toLowerCase());
    }
    return new IdentityDataTypeTransformer();
  }
}
