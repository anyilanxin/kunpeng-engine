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
package com.anyilanxin.kunpeng.structpack.property;

import com.anyilanxin.kunpeng.structpack.StructPackException;
import com.anyilanxin.kunpeng.structpack.value.DocumentValue;
import org.agrona.DirectBuffer;

/**
 * 文档属性（流程变量等）：内容为标准 msgpack 字节，本类只透传不解析。
 *
 * <p>解析/生成由 {@code DocumentUtil}(Jackson) 完成——存量变量字节与外部 API 完全兼容。
 */
public class DocumentProperty extends BaseProperty<DocumentValue> {

  private static DocumentValue emptyDocument() {
    return new DocumentValue(
        DocumentValue.EMPTY_DOCUMENT, 0, DocumentValue.EMPTY_DOCUMENT.capacity());
  }

  public DirectBuffer getValue() {
    return resolveValue().getValue();
  }

  public DocumentProperty setValue(final DirectBuffer data) {
    return setValue(data, 0, data.capacity());
  }

  public DocumentProperty setValue(final DirectBuffer data, final int offset, final int length) {
    try {
      this.value.wrap(data, offset, length);
      this.isSet = true;
    } catch (final Exception e) {
      throw new StructPackException(key, e.getMessage());
    }
    return this;
  }

  public DocumentProperty(final int id, final String key) {
    super(id, key, new DocumentValue(), emptyDocument());
  }
}
