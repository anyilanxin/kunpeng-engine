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
package com.anyilanxin.kunpeng.cluster.utils.serializer.serializers;

import static org.assertj.core.api.Assertions.assertThat;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Sets up a Kryo instance where we write/read from a shared buffer. */
public class ByteBufferSerializerTest {
  private static final int CAPACITY = 1024;
  private static final Kryo KRYO = new Kryo();

  private Output output;
  private Input input;
  private ByteBuffer buffer;

  @BeforeClass
  public static void register() {
    KRYO.register(ByteBuffer.class, new ByteBufferSerializer());
    KRYO.register(ByteBuffer.allocate(1).getClass(), new ByteBufferSerializer());
    KRYO.register(ByteBuffer.allocateDirect(1).getClass(), new ByteBufferSerializer());
    KRYO.addDefaultSerializer(ByteBuffer.class, new ByteBufferSerializer());
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
  public void shouldSerializeRemainingOnly() {
    // given
    final int value = 1;
    final int capacity = Long.BYTES;
    final ByteBuffer original = ByteBuffer.allocate(capacity * 2).putLong(value * 2).putLong(value);

    // when
    original.position(capacity);
    KRYO.writeObject(output, original);
    buffer.flip();
    final ByteBuffer deserialized = KRYO.readObject(input, ByteBuffer.class);

    // then
    assertThat(deserialized.remaining()).isEqualTo(capacity);
    assertThat(deserialized.capacity()).isEqualTo(capacity);
    assertThat(deserialized.getLong(0)).isEqualTo(value);
  }

  @Test
  public void shouldSerializeDirectBuffer() {
    // given
    final int value = 1;
    final int capacity = Long.BYTES;
    final ByteBuffer original = ByteBuffer.allocateDirect(capacity).putLong(0, value);

    // when
    KRYO.writeObject(output, original);
    buffer.flip();
    final ByteBuffer deserialized = KRYO.readObject(input, ByteBuffer.class);

    // then
    assertThat(deserialized.remaining()).isEqualTo(capacity);
    assertThat(deserialized.capacity()).isEqualTo(capacity);
    assertThat(deserialized.isDirect()).isTrue();
    assertThat(deserialized.getLong(0)).isEqualTo(value);
  }

  @Test
  public void shouldSerializeHeapBuffer() {
    // given
    final int value = 1;
    final int capacity = Long.BYTES;
    final ByteBuffer original = ByteBuffer.allocate(capacity).putLong(0, value);

    // when
    KRYO.writeObject(output, original);
    buffer.flip();
    final ByteBuffer deserialized = KRYO.readObject(input, ByteBuffer.class);

    // then
    assertThat(deserialized.remaining()).isEqualTo(capacity);
    assertThat(deserialized.capacity()).isEqualTo(capacity);
    assertThat(deserialized.isDirect()).isFalse();
    assertThat(deserialized.getLong(0)).isEqualTo(value);
  }

  @Test
  public void shouldSerializeLittleEndianBuffer() {
    // given
    final int value = 1;
    final int capacity = Long.BYTES;
    final ByteBuffer original =
        ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN).putLong(0, value);

    // when
    KRYO.writeObject(output, original);
    buffer.flip();
    final ByteBuffer deserialized = KRYO.readObject(input, ByteBuffer.class);

    // then
    assertThat(deserialized.order()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
    assertThat(deserialized.getLong(0)).isEqualTo(value);
  }

  @Test
  public void shouldSerializeBigEndianBuffer() {
    // given
    final int value = 1;
    final int capacity = Long.BYTES;
    final ByteBuffer original =
        ByteBuffer.allocate(capacity).order(ByteOrder.BIG_ENDIAN).putLong(0, value);

    // when
    KRYO.writeObject(output, original);
    buffer.flip();
    final ByteBuffer deserialized = KRYO.readObject(input, ByteBuffer.class);

    // then
    assertThat(deserialized.order()).isEqualTo(ByteOrder.BIG_ENDIAN);
    assertThat(deserialized.getLong(0)).isEqualTo(value);
  }

  @Test
  public void shouldSerializeBufferWithNonZeroPositionAndLimit() {
    // given
    final int capacity = Integer.BYTES * 4;
    final int firstPosition = Integer.BYTES;
    final int firstValue = 1;
    final int secondPosition = Integer.BYTES;
    final int secondValue = 2;
    final ByteBuffer original =
        ByteBuffer.allocate(capacity)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(firstPosition, firstValue)
            .putInt(secondPosition, secondValue);

    // when
    original.position(secondPosition).limit(secondPosition + Integer.BYTES);
    KRYO.writeObject(output, original);
    buffer.flip();
    final ByteBuffer deserialized = KRYO.readObject(input, ByteBuffer.class);

    // then
    assertThat(deserialized.order()).isEqualTo(ByteOrder.BIG_ENDIAN);
    assertThat(deserialized.getInt(0)).isEqualTo(secondValue);
  }

  @Test
  public void shouldSerializeZeroLengthBuffers() {
    // given
    final int capacity = Integer.BYTES;
    final int value = 1;
    final ByteBuffer original =
        ByteBuffer.allocate(capacity).order(ByteOrder.BIG_ENDIAN).putInt(0, value);

    // when
    original.position(capacity);
    KRYO.writeObject(output, original);
    buffer.flip();
    final ByteBuffer deserialized = KRYO.readObject(input, ByteBuffer.class);

    // then
    assertThat(deserialized.order()).isEqualTo(ByteOrder.BIG_ENDIAN);
    assertThat(deserialized.capacity()).isEqualTo(0);
  }
}
