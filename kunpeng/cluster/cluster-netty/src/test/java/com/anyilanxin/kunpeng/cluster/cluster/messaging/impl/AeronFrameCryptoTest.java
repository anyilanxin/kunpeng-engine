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
package com.anyilanxin.kunpeng.cluster.cluster.messaging.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Map;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

/**
 * {@link AeronFrameCrypto} 单元测试：往返、IV 唯一性、防篡改、密钥轮换与配置校验。
 *
 * <p>注意 {@code encrypt} 返回的缓冲复用内部暂存区：每次加密后立即复制出信封再断言/篡改。
 */
final class AeronFrameCryptoTest {

  private static final String KEY_V1 = "0123456789abcdef".repeat(4);
  private static final String KEY_V2 = "fedcba9876543210".repeat(4);

  private final SecureRandom random = new SecureRandom();

  @Test
  void roundTripsVariousFrameSizes() {
    final AeronFrameCrypto nodeA = new AeronFrameCrypto(Map.of(1, KEY_V1), 1, "node-a");
    final AeronFrameCrypto nodeB = new AeronFrameCrypto(Map.of(1, KEY_V1), 1, "node-b");
    for (final int size : new int[] {16, 512, 4096, 64 * 1024, 512 * 1024}) {
      final byte[] plain = randomBytes(size);
      assertArrayEquals(plain, decryptToBytes(nodeB, encryptCopy(nodeA, plain)), "size=" + size);
    }
  }

  @Test
  void envelopeCarriesMagicAndUniqueIvPerMessage() {
    final AeronFrameCrypto crypto = new AeronFrameCrypto(Map.of(1, KEY_V1), 1, "node-a");
    final byte[] plain = randomBytes(64);
    final byte[] first = encryptCopy(crypto, plain);
    final byte[] second = encryptCopy(crypto, plain);

    assertEquals(0xAE, first[0] & 0xFF, "信封首字节应为 magic 0xAE");
    final int senderLen = first[1] & 0xFF;
    assertEquals("node-a".length(), senderLen);
    assertEquals(
        plain.length + 16, first.length - (2 + senderLen + 12), "密文长度 = 明文 + GCM tag");
    // 同一明文两次加密: IV 计数器不同 → 信封不同, 且密文整体不同
    assertNotEquals(java.util.Arrays.hashCode(first), java.util.Arrays.hashCode(second));
    final UnsafeBuffer firstView = new UnsafeBuffer(first);
    final UnsafeBuffer secondView = new UnsafeBuffer(second);
    final long firstCounter = firstView.getLong(2 + senderLen + 4);
    final long secondCounter = secondView.getLong(2 + senderLen + 4);
    assertNotEquals(firstCounter, secondCounter, "每次加密必须消耗新的 IV 计数器");
  }

  @Test
  void defaultSendKeyIsTheHighestGeneration() {
    final AeronFrameCrypto crypto = new AeronFrameCrypto(Map.of(1, KEY_V1, 7, KEY_V2), -1, "node-a");
    assertEquals(7, crypto.sendKeyId());
    final byte[] envelope = encryptCopy(crypto, randomBytes(32));
    final UnsafeBuffer view = new UnsafeBuffer(envelope);
    final int senderLen = envelope[1] & 0xFF;
    assertEquals(7, view.getInt(2 + senderLen), "信封 IV 中应携带发送密钥世代 keyId");
  }

  @Test
  void tamperedCiphertextIsDropped() {
    tamperedEnvelopeIsDropped("ciphertext", envelope -> envelope[envelope.length - 20] ^= 0x5A);
  }

  @Test
  void tamperedTagIsDropped() {
    tamperedEnvelopeIsDropped("tag", envelope -> envelope[envelope.length - 1] ^= 0x01);
  }

  @Test
  void forgedSenderIdIsDropped() {
    // 篡改 senderId → 接收方派生出不同的发送密钥 → AEAD 认证失败
    tamperedEnvelopeIsDropped("senderId", envelope -> envelope[2] ^= 0x20);
  }

  @Test
  void wrongMagicOrTruncatedEnvelopeIsDropped() {
    final AeronFrameCrypto sender = new AeronFrameCrypto(Map.of(1, KEY_V1), 1, "node-a");
    final AeronFrameCrypto receiver = new AeronFrameCrypto(Map.of(1, KEY_V1), 1, "node-b");
    final byte[] envelope = encryptCopy(sender, randomBytes(64));

    final byte[] badMagic = envelope.clone();
    badMagic[0] = 0x01;
    assertNull(receiver.decrypt(new UnsafeBuffer(badMagic), 0, badMagic.length));

    assertNull(
        receiver.decrypt(new UnsafeBuffer(envelope, 0, 10), 0, 10), "截断信封应被丢弃");
    assertNull(
        receiver.decrypt(new UnsafeBuffer(envelope, 0, 2 + "node-a".length() + 12), 0,
            2 + "node-a".length() + 12),
        "只有信封头没有密文的帧应被丢弃");
  }

  @Test
  void unknownKeyGenerationIsDroppedDuringRotationDrift() {
    // 发送方已切到世代 2, 接收方只有世代 1(轮换未完成) → 丢弃而非解出错误数据
    final AeronFrameCrypto sender = new AeronFrameCrypto(Map.of(1, KEY_V1, 2, KEY_V2), 2, "node-a");
    final AeronFrameCrypto laggingReceiver =
        new AeronFrameCrypto(Map.of(1, KEY_V1), 1, "node-b");
    final byte[] envelope = encryptCopy(sender, randomBytes(64));
    assertNull(laggingReceiver.decrypt(new UnsafeBuffer(envelope), 0, envelope.length));

    // 同时持有两个世代的接收方正常解密
    final AeronFrameCrypto currentReceiver =
        new AeronFrameCrypto(Map.of(1, KEY_V1, 2, KEY_V2), 1, "node-b");
    assertNotNull(currentReceiver.decrypt(new UnsafeBuffer(envelope), 0, envelope.length));
  }

  @Test
  void wrongPskWithSameKeyIdIsDropped() {
    final AeronFrameCrypto sender = new AeronFrameCrypto(Map.of(1, KEY_V1), 1, "node-a");
    final AeronFrameCrypto stranger = new AeronFrameCrypto(Map.of(1, KEY_V2), 1, "node-b");
    final byte[] envelope = encryptCopy(sender, randomBytes(64));
    assertNull(stranger.decrypt(new UnsafeBuffer(envelope), 0, envelope.length));
  }

  @Test
  void plaintextFrameIsDetectableForMixedModeDispatch() {
    final byte[] plaintextFrame = new byte[32];
    plaintextFrame[0] = 0x01;
    assertTrue(
        !AeronFrameCrypto.isEncryptedFrame(new UnsafeBuffer(plaintextFrame), 0),
        "明文帧(版本字节 0x01)不应被识别为加密信封");
    final AeronFrameCrypto crypto = new AeronFrameCrypto(Map.of(1, KEY_V1), 1, "node-a");
    final byte[] envelope = encryptCopy(crypto, plaintextFrame);
    assertTrue(AeronFrameCrypto.isEncryptedFrame(new UnsafeBuffer(envelope), 0));
  }

  @Test
  void rejectsInvalidKeyConfiguration() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AeronFrameCrypto(Map.of(), 1, "node-a"),
        "空密钥表应拒绝启动");
    assertThrows(
        IllegalArgumentException.class,
        () -> new AeronFrameCrypto(Map.of(1, "zzzz"), 1, "node-a"),
        "非十六进制密钥应拒绝");
    assertThrows(
        IllegalArgumentException.class,
        () -> new AeronFrameCrypto(Map.of(1, KEY_V1.substring(2)), 1, "node-a"),
        "非 256-bit 密钥应拒绝");
    assertThrows(
        IllegalArgumentException.class,
        () -> new AeronFrameCrypto(Map.of(1, KEY_V1), 9, "node-a"),
        "发送密钥不在密钥表中应拒绝");
    assertThrows(
        IllegalArgumentException.class,
        () -> new AeronFrameCrypto(Map.of(1, KEY_V1), 1, "x".repeat(256)),
        "超长发送方标识应拒绝");
  }

  private void tamperedEnvelopeIsDropped(final String region, final java.util.function.Consumer<byte[]> tamper) {
    final AeronFrameCrypto sender = new AeronFrameCrypto(Map.of(1, KEY_V1), 1, "node-a");
    final AeronFrameCrypto receiver = new AeronFrameCrypto(Map.of(1, KEY_V1), 1, "node-b");
    final byte[] envelope = encryptCopy(sender, randomBytes(64));
    tamper.accept(envelope);
    assertNull(
        receiver.decrypt(new UnsafeBuffer(envelope), 0, envelope.length),
        "篡改 " + region + " 后的帧必须被丢弃");
  }

  /** 加密并复制出信封字节（返回缓冲复用暂存区，不能直接持有）。 */
  private static byte[] encryptCopy(final AeronFrameCrypto crypto, final byte[] plain) {
    // encrypt 返回任务自持的新数组, 无需再复制
    return crypto.encrypt(new UnsafeBuffer(plain), plain.length);
  }

  private static byte[] decryptToBytes(final AeronFrameCrypto crypto, final byte[] envelope) {
    final UnsafeBuffer plain = crypto.decrypt(new UnsafeBuffer(envelope), 0, envelope.length);
    assertNotNull(plain, "合法信封必须解密成功");
    final byte[] copy = new byte[plain.capacity()];
    plain.getBytes(0, copy);
    return copy;
  }

  private byte[] randomBytes(final int size) {
    final byte[] bytes = new byte[size];
    random.nextBytes(bytes);
    return bytes;
  }
}
