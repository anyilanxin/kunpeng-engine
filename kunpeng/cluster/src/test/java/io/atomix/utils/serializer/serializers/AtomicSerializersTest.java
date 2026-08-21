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
package io.atomix.utils.serializer.serializers;

import static org.assertj.core.api.Assertions.assertThat;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AtomicSerializersTest {
  private static final int CAPACITY = 1024;
  private static final Kryo KRYO = new Kryo();

  private Output output;
  private Input input;
  private ByteBuffer buffer;

  @BeforeClass
  public static void register() {
    KRYO.register(AtomicLong.class, new AtomicLongSerializer());
    KRYO.register(AtomicInteger.class, new AtomicIntegerSerializer());
    KRYO.register(AtomicBoolean.class, new AtomicBooleanSerializer());
  }

  @Before
  public void setUp() {
    buffer = ByteBuffer.allocate(CAPACITY);

    output = new ByteBufferOutput(buffer);
    input = new ByteBufferInput(buffer);
  }

  @After
  public void tearDown() {
    output.close();
    input.close();
  }

  @Test
  public void shouldSerializeDeserializeLong() {
    // given
    final AtomicLong original = new AtomicLong(1);

    // when
    original.set(32L);
    KRYO.writeObject(output, original);
    buffer.flip();

    final AtomicLong deserialized = KRYO.readObject(input, AtomicLong.class);

    // then
    assertThat(deserialized.get()).isEqualTo(32L);
  }

  @Test
  public void shouldSerializeDeserializeInteger() {
    // given
    final AtomicInteger original = new AtomicInteger(1);

    // when
    original.set(1000);
    KRYO.writeObject(output, original);
    buffer.flip();
    final AtomicInteger deserialized = KRYO.readObject(input, AtomicInteger.class);

    // then
    assertThat(deserialized.get()).isEqualTo(1000);
  }

  @Test
  public void shouldSerializeDeserializeBoolean() {
    // given
    final AtomicBoolean original = new AtomicBoolean(false);

    // when
    original.set(true);
    KRYO.writeObject(output, original);
    buffer.flip();
    final AtomicBoolean deserialized = KRYO.readObject(input, AtomicBoolean.class);

    // then
    assertThat(deserialized.get()).isTrue();
  }
}
