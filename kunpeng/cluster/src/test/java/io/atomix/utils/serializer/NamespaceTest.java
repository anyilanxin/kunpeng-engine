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
package io.atomix.utils.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
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
        new Namespace.Builder().register(new NumberSerializer(), Integer.class, Long.class).build();
    final Long expectedLong = 5L;
    final Integer expectedInteger = 7;

    // when
    final Long gotLong = ns.deserialize(ns.serialize(expectedLong));
    final Integer gotInteger = ns.deserialize(ns.serialize(expectedInteger));

    // then
    assertThat(gotLong).isEqualTo(expectedLong);
    assertThat(gotInteger).isEqualTo(expectedInteger);
  }

  private static final class NumberSerializer extends Serializer<Number> {

    @Override
    public void write(final Kryo kryo, final Output output, final Number object) {
      if (Integer.class.equals(object.getClass())) {
        output.write(object.intValue());
      } else {
        output.writeLong(object.longValue());
      }
    }

    @Override
    public Number read(final Kryo kryo, final Input input, final Class<? extends Number> type) {
      if (Integer.class.equals(type)) {
        return input.read();
      } else {
        return input.readLong();
      }
    }
  }
}
