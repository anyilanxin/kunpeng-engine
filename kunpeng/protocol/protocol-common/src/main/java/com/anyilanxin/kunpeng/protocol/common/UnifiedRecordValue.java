/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.protocol.common;

import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertJsonSerializableObjectToJson;

import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 统一 Record 值基类：所有 Record 的 structpack 序列化基座。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("rawtypes")
public class UnifiedRecordValue<T extends UnifiedRecordValue> extends UnpackedObject
    implements RecordValue {

  public UnifiedRecordValue(final int capacity) {
    super(capacity);
  }

  public UnifiedRecordValue() {}

  @Override
  @JsonIgnore
  public int getLength() {
    return super.getLength();
  }

  @Override
  @JsonIgnore
  public int getEncodedLength() {
    return super.getEncodedLength();
  }

  @Override
  @JsonIgnore
  public boolean isEmpty() {
    return super.isEmpty();
  }

  @Override
  public String toJson() {
    return convertJsonSerializableObjectToJson(this);
  }

  public T copy() {
    final var valueBuffer = new UnsafeBuffer(new byte[getLength()]);
    write(valueBuffer, 0);
    final T recordValue = newRecord();
    recordValue.wrap(valueBuffer, 0, valueBuffer.capacity());
    return recordValue;
  }

  protected T newRecord() {
    throw new IllegalArgumentException("未实现创建新对象");
  }
}
