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
package com.anyilanxin.kunpeng.repository.business.modules.sink.record;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.BinaryProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * sink 状态 Entity：sink 位置与状态信息的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class SinkStateEntry extends UnpackedObject implements StoreValue {

  private static final UnsafeBuffer EMPTY_METADATA = new UnsafeBuffer();

  private final LongProperty positionProp = new LongProperty(1, "sinkPosition");
  private final BinaryProperty metadataProp = new BinaryProperty(2, "sinkMetadata", EMPTY_METADATA);

  private final LongProperty metadataVersionProp = new LongProperty(3, "metadataVersion", 0L);

  public SinkStateEntry() {
    super(3);
    declareProperty(positionProp)
        .declareProperty(metadataProp)
        .declareProperty(metadataVersionProp);
  }

  public long getPosition() {
    return positionProp.getValue();
  }

  public SinkStateEntry setPosition(final long position) {
    positionProp.setValue(position);
    return this;
  }

  public DirectBuffer getMetadata() {
    // Clone the buffer to avoid misuse. The buffer is reused by the state.
    return BufferUtil.cloneBuffer(metadataProp.getValue());
  }

  public SinkStateEntry setMetadata(final DirectBuffer metadata) {
    metadataProp.setValue(metadata);
    return this;
  }

  public long getMetadataVersion() {
    return metadataVersionProp.getValue();
  }

  public SinkStateEntry setMetadataVersion(final long metadataVersion) {
    metadataVersionProp.setValue(metadataVersion);
    return this;
  }
}
