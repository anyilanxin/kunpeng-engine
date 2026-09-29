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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.fory.config.Config;
import org.apache.fory.context.ReadContext;
import org.apache.fory.context.WriteContext;
import org.apache.fory.serializer.Serializer;
import org.junit.Test;

public class NamespaceTest {

  @Test
  public void shouldDeserializeObject() {
    // given
    final Namespace ns = new Namespace.Builder().register(Integer.class).build();
    final Integer want = 99;

    // when
    final byte[] ser = ns.serialize(want);
    final Object got = ns.deserialize(ser);

    // then
    assertThat(got).isEqualTo(want);
  }

  @Test
  public void shouldRegisterMultipleTypesSimultaneously() {
    // given
    final Namespace ns =
        new Namespace.Builder().register(NumberSerializer.class, Integer.class, Long.class).build();
    final Long expectedLong = 5L;
    final Integer expectedInteger = 7;

    // when
    final Long gotLong = ns.deserialize(ns.serialize(expectedLong));
    final Integer gotInteger = ns.deserialize(ns.serialize(expectedInteger));

    // then
    assertThat(gotLong).isEqualTo(expectedLong);
    assertThat(gotInteger).isEqualTo(expectedInteger);
  }

  /** Number 测试序列化器：Fory read() 无类型参数，改为首字节写形态标记（0=Int，1=Long）。 */
  private static final class NumberSerializer extends Serializer<Number> {

    private static final byte INT_FLAG = 0;
    private static final byte LONG_FLAG = 1;

    // Fory 反射实例化，构造器必须 public
    public NumberSerializer(final Config config) {
      super(config, Number.class);
    }

    @Override
    public void write(final WriteContext writeContext, final Number object) {
      if (Integer.class.equals(object.getClass())) {
        writeContext.getBuffer().writeByte(INT_FLAG);
        writeContext.writeInt32(object.intValue());
      } else {
        writeContext.getBuffer().writeByte(LONG_FLAG);
        writeContext.writeInt64(object.longValue());
      }
    }

    @Override
    public Number read(final ReadContext readContext) {
      if (readContext.getBuffer().readByte() == INT_FLAG) {
        return readContext.readInt32();
      }
      return readContext.readInt64();
    }
  }
}
