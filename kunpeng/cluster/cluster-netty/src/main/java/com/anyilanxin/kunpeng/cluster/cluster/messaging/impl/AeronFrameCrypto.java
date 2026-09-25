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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用层帧加密：AES-256-GCM 信封，在 {@link AeronTransport} 的发送/接收两个咽喉处整体加解密帧。
 *
 * <p>信封布局（小端，与 {@link AeronFrameCodec} 的字节序约定一致）：
 *
 * <pre>
 * [0xAE magic][senderIdLen(1)][senderId UTF-8][IV 12B = keyId(4) + counter(8)][密文 = 明文帧 || GCM tag(16)]
 * </pre>
 *
 * <p>密钥体系：配置提供集群共享 PSK（keyId → 256-bit hex，keyId 用于滚动轮换，接收侧兼容多个世代）。每个发送方 的实际 AES 密钥由 PSK 经
 * HKDF-SHA256 按 senderId 派生——同一 PSK 下各成员的 IV 空间互不重叠（GCM 的 IV 唯一性只约束单发送方）， 计数器以随机 64
 * 位起点单调递增，进程重启后碰撞概率可忽略。注意：共享 PSK 提供机密性与「对外完整性」，不提供成员间 互相认证（持有 PSK 者可伪造任意
 * senderId），需要成员认证时应升级为每成员密钥或证书协商。
 *
 * <p>明文帧首字节恒为 {@link AeronFrameCodec#VERSION}（0x01），与信封 magic 0xAE 可区分，因此支持「部分节点已启用加密」
 * 的滚动升级窗口：未启用侧收到的 0x01 帧按明文处理，启用侧对 0x01 帧同样放行。
 *
 * <p>线程约束：实例状态（Cipher、计数器、密钥缓存、暂存数组）全部封闭在传输代理线程，非线程安全；解密返回的缓冲 复用内部暂存区， 仅在当前回调内有效（与 Aeron
 * 片段回调语义一致，监听器不得持有引用）。
 */
final class AeronFrameCrypto {

  private static final Logger LOGGER = LoggerFactory.getLogger(AeronFrameCrypto.class);

  /** 加密信封 magic，区别于明文帧的 {@link AeronFrameCodec#VERSION}。 */
  static final byte ENVELOPE_MAGIC = (byte) 0xAE;

  private static final int KEY_BYTES = 32;
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 16 * Byte.SIZE;
  private static final int MAX_SENDER_ID_BYTES = 255;
  private static final int MIN_ENVELOPE_BYTES = 2 + IV_BYTES + 16;
  private static final byte[] HKDF_SALT = "kunpeng-cluster/v1".getBytes(StandardCharsets.UTF_8);
  private static final byte[] HKDF_INFO_PREFIX =
      "aeron-frame-sender/".getBytes(StandardCharsets.UTF_8);

  private final Map<Integer, SecretKeySpec> clusterKeys;
  private final Map<String, SecretKeySpec> senderKeys = new HashMap<>();
  private final int sendKeyId;
  private final byte[] senderIdBytes;
  private final Cipher encryptCipher = cipher("AES/GCM/NoPadding");
  private final Cipher decryptCipher = cipher("AES/GCM/NoPadding");
  private final byte[] iv = new byte[IV_BYTES];
  private final UnsafeBuffer ivView = new UnsafeBuffer(iv);
  private long counter = new SecureRandom().nextLong();
  private byte[] cipherScratch = new byte[512];
  private byte[] plainScratch = new byte[512];
  private long droppedFrames;

  AeronFrameCrypto(
      final Map<Integer, String> clusterKeysHex, final int sendKeyId, final String senderId) {
    if (clusterKeysHex == null || clusterKeysHex.isEmpty()) {
      throw new IllegalArgumentException("启用传输加密必须至少配置一个密钥(addCryptoKey: keyId → 64 位十六进制字符)");
    }
    this.clusterKeys = new HashMap<>();
    clusterKeysHex.forEach(
        (keyId, keyHex) ->
            clusterKeys.put(keyId, new SecretKeySpec(parseKey(keyId, keyHex), "AES")));
    int active = sendKeyId;
    if (active < 0) {
      // 默认取最大 keyId：轮换时先向全集群分发新密钥，配置重载后发送侧自动切到新世代
      active = Collections.max(clusterKeys.keySet());
    }
    if (!clusterKeys.containsKey(active)) {
      throw new IllegalArgumentException(
          "发送密钥 keyId=" + active + " 不在配置的密钥集合 " + clusterKeys.keySet() + " 中");
    }
    this.sendKeyId = active;
    this.senderIdBytes = senderId.getBytes(StandardCharsets.UTF_8);
    if (senderIdBytes.length > MAX_SENDER_ID_BYTES) {
      throw new IllegalArgumentException("加密发送方标识过长(>255 字节): " + senderId);
    }
  }

  /** 该帧是否为加密信封（magic 字节判定）。 */
  static boolean isEncryptedFrame(final DirectBuffer frame, final int offset) {
    return frame.getByte(offset) == ENVELOPE_MAGIC;
  }

  int sendKeyId() {
    return sendKeyId;
  }

  /**
   * 加密一帧，返回任务自持的密文信封数组。
   *
   * <p>每次调用分配独立数组（调用方缓存于任务上、重试复用）。绝不能用内部暂存区返回视图: 挂起重试中的任务 会与其他帧的加密互相踩踏, 接收方将 AEAD 认证失败并丢弃。
   */
  byte[] encrypt(final DirectBuffer frame, final int length) {
    final byte[] array = frame.byteArray();
    if (array != null && array.length == length) {
      return encrypt(array);
    }
    // 非精确数组承载的帧（防御路径，常规发送帧均为等长 byte[]）复制后再加密
    final byte[] copy = new byte[length];
    frame.getBytes(0, copy, 0, length);
    return encrypt(copy);
  }

  private byte[] encrypt(final byte[] plainFrame) {
    final int headerLength = 2 + senderIdBytes.length + IV_BYTES;
    final byte[] envelope = new byte[headerLength + plainFrame.length + 16];
    final UnsafeBuffer view = new UnsafeBuffer(envelope);
    view.putByte(0, ENVELOPE_MAGIC);
    view.putByte(1, (byte) senderIdBytes.length);
    view.putBytes(2, senderIdBytes);
    final int ivOffset = 2 + senderIdBytes.length;
    ivView.putInt(0, sendKeyId);
    ivView.putLong(4, ++counter);
    view.putBytes(ivOffset, iv);
    try {
      encryptCipher.init(
          Cipher.ENCRYPT_MODE,
          senderKey(sendKeyId, senderIdBytes),
          new GCMParameterSpec(TAG_BITS, iv));
      final int written =
          encryptCipher.doFinal(plainFrame, 0, plainFrame.length, envelope, headerLength);
      if (written != plainFrame.length + 16) {
        throw new IllegalStateException("GCM 输出长度异常: " + written);
      }
    } catch (final GeneralSecurityException error) {
      throw new IllegalStateException("帧加密失败", error);
    }
    return envelope;
  }

  /**
   * 解密一帧；认证失败、密钥未知或信封畸形时返回 null（调用方丢弃该帧）。
   *
   * <p>返回的缓冲复用内部暂存区，仅在本回调内有效。
   */
  UnsafeBuffer decrypt(final DirectBuffer frame, final int offset, final int length) {
    if (length < MIN_ENVELOPE_BYTES || frame.getByte(offset) != ENVELOPE_MAGIC) {
      return drop("信封长度或 magic 非法");
    }
    final int senderIdLength = frame.getByte(offset + 1) & 0xFF;
    final int headerLength = 2 + senderIdLength + IV_BYTES;
    if (length < headerLength + 16) {
      return drop("信封头长度非法: senderIdLength=" + senderIdLength);
    }
    final byte[] senderId = new byte[senderIdLength];
    frame.getBytes(offset + 2, senderId);
    final int keyId = frame.getInt(offset + 2 + senderIdLength);
    final SecretKeySpec psk = clusterKeys.get(keyId);
    if (psk == null) {
      return drop("未知密钥世代 keyId=" + keyId + "（密钥轮换未完成或对端配置漂移）");
    }
    final int cipherLength = length - headerLength;
    cipherScratch = ensure(cipherScratch, cipherLength);
    frame.getBytes(offset + headerLength, cipherScratch, 0, cipherLength);
    frame.getBytes(offset + 2 + senderIdLength, iv, 0, IV_BYTES);
    plainScratch = ensure(plainScratch, cipherLength);
    try {
      decryptCipher.init(
          Cipher.DECRYPT_MODE, senderKey(psk, keyId, senderId), new GCMParameterSpec(TAG_BITS, iv));
      final int plainLength =
          decryptCipher.doFinal(cipherScratch, 0, cipherLength, plainScratch, 0);
      return new UnsafeBuffer(plainScratch, 0, plainLength);
    } catch (final AEADBadTagException error) {
      return drop("AEAD 认证失败（数据被篡改、PSK 不一致或 senderId 被伪造）");
    } catch (final GeneralSecurityException error) {
      return drop("解密异常: " + error);
    }
  }

  /** 解密失败统一走此路径：首次与每 100 帧记一条日志，避免对端配置漂移时刷屏。 */
  private UnsafeBuffer drop(final String reason) {
    droppedFrames++;
    if (droppedFrames == 1 || droppedFrames % 100 == 0) {
      LOGGER.warn("丢弃无法解密的帧: {}（累计 {} 帧）", reason, droppedFrames);
    }
    return null;
  }

  /** 发送密钥按 (keyId, senderId) 缓存派生结果。 */
  private SecretKeySpec senderKey(final int keyId, final byte[] senderId) {
    final SecretKeySpec psk = clusterKeys.get(keyId);
    if (psk == null) {
      throw new IllegalStateException("发送密钥世代 keyId=" + keyId + " 未知");
    }
    return senderKey(psk, keyId, senderId);
  }

  private SecretKeySpec senderKey(final SecretKeySpec psk, final int keyId, final byte[] senderId) {
    final String cacheKey = keyId + "/" + new String(senderId, StandardCharsets.UTF_8);
    return senderKeys.computeIfAbsent(cacheKey, ignored -> deriveSenderKey(psk, senderId));
  }

  /** HKDF-SHA256(extract(salt, PSK), expand(info = "aeron-frame-sender/" + senderId, 32))。 */
  private static SecretKeySpec deriveSenderKey(final SecretKeySpec psk, final byte[] senderId) {
    try {
      final Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(HKDF_SALT, "HmacSHA256"));
      final byte[] prk = mac.doFinal(psk.getEncoded());
      mac.init(new SecretKeySpec(prk, "HmacSHA256"));
      mac.update(HKDF_INFO_PREFIX);
      mac.update(senderId);
      mac.update((byte) 1);
      return new SecretKeySpec(mac.doFinal(), "AES");
    } catch (final GeneralSecurityException error) {
      throw new IllegalStateException("HKDF 派生发送密钥失败", error);
    }
  }

  private static byte[] parseKey(final Integer keyId, final String keyHex) {
    if (keyHex == null) {
      throw new IllegalArgumentException("密钥 keyId=" + keyId + " 内容为空");
    }
    final byte[] key;
    try {
      key = HexFormat.of().parseHex(keyHex);
    } catch (final IllegalArgumentException error) {
      throw new IllegalArgumentException("密钥 keyId=" + keyId + " 不是合法十六进制: " + error.getMessage());
    }
    if (key.length != KEY_BYTES) {
      throw new IllegalArgumentException(
          "密钥 keyId=" + keyId + " 必须为 256-bit(64 个十六进制字符), 实际 " + key.length + " 字节");
    }
    return key;
  }

  private static Cipher cipher(final String transformation) {
    try {
      return Cipher.getInstance(transformation);
    } catch (final GeneralSecurityException error) {
      throw new IllegalStateException("JVM 不支持 " + transformation, error);
    }
  }

  private static byte[] ensure(final byte[] buffer, final int required) {
    return buffer.length >= required ? buffer : new byte[Math.max(required, buffer.length * 2)];
  }
}
