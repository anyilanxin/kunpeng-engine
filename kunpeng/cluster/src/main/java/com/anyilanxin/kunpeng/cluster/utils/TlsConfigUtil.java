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
package com.anyilanxin.kunpeng.cluster.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PKCS#12 密钥库的 TLS 材料读取工具。
 *
 * <p>服务端身份（私钥 + 证书链）用 {@link #loadServerIdentity(Path, char[])} 一次打开密钥库同时取出，
 * 避免同一文件读两遍；客户端信任材料用 {@link #readTrustedCertificates(Path, char[])}，只要求证书、
 * 不要求密钥库中存在私钥条目（纯信任库场景）。
 */
public final class TlsConfigUtil {

  private static final String PKCS12 = "PKCS12";

  private TlsConfigUtil() {}

  /** 服务端 TLS 身份：私钥与其证书链。 */
  public record ServerIdentity(PrivateKey privateKey, X509Certificate[] certificateChain) {}

  /**
   * 打开密钥库并一次取出私钥与证书链。
   *
   * @param keyStoreFile PKCS#12 密钥库文件
   * @param password 密钥库口令；无口令密钥库传 {@code null}
   * @throws KeyStoreException 密钥库中没有私钥条目，或条目缺少证书链
   */
  public static ServerIdentity loadServerIdentity(final Path keyStoreFile, final char[] password)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException,
          UnrecoverableKeyException {
    final var keyStore = openPkcs12(keyStoreFile, password);
    final String alias = privateKeyAlias(keyStore, keyStoreFile);

    final var key = keyStore.getKey(alias, password);
    if (!(key instanceof final PrivateKey privateKey)) {
      throw new KeyStoreException(
          "Entry '" + alias + "' of keystore " + keyStoreFile + " holds no private key");
    }
    return new ServerIdentity(privateKey, x509ChainOf(keyStore, alias, keyStoreFile));
  }

  /**
   * 读取密钥库中全部证书并扁平化为信任列表；不要求存在私钥条目。
   *
   * @param keyStoreFile PKCS#12 密钥库文件
   * @param password 密钥库口令；无口令密钥库传 {@code null}
   * @throws KeyStoreException 密钥库中一张证书都没有
   */
  public static X509Certificate[] readTrustedCertificates(
      final Path keyStoreFile, final char[] password)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    final var keyStore = openPkcs12(keyStoreFile, password);
    final List<X509Certificate> trusted = new ArrayList<>();
    for (final String alias : Collections.list(keyStore.aliases())) {
      final Certificate[] chain = keyStore.getCertificateChain(alias);
      if (chain != null) {
        collectX509(chain, trusted, alias, keyStoreFile);
      } else {
        final Certificate single = keyStore.getCertificate(alias);
        if (single instanceof final X509Certificate x509) {
          trusted.add(x509);
        }
      }
    }
    if (trusted.isEmpty()) {
      throw new KeyStoreException("Keystore " + keyStoreFile + " contains no certificate at all");
    }
    return trusted.toArray(X509Certificate[]::new);
  }

  private static KeyStore openPkcs12(final Path file, final char[] password)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    final var keyStore = KeyStore.getInstance(PKCS12);
    try (final InputStream stream = Files.newInputStream(file)) {
      keyStore.load(stream, password);
    }
    return keyStore;
  }

  /** 找到第一个私钥条目的别名；没有私钥条目视为密钥库配置错误。 */
  private static String privateKeyAlias(final KeyStore keyStore, final Path file)
      throws KeyStoreException {
    for (final String alias : Collections.list(keyStore.aliases())) {
      if (keyStore.isKeyEntry(alias)) {
        return alias;
      }
    }
    throw new KeyStoreException("Keystore " + file + " has no private key entry");
  }

  /** 读取指定条目的证书链并校验全部为 X.509 证书。 */
  private static X509Certificate[] x509ChainOf(
      final KeyStore keyStore, final String alias, final Path file) throws KeyStoreException {
    final Certificate[] chain = keyStore.getCertificateChain(alias);
    if (chain == null || chain.length == 0) {
      throw new KeyStoreException(
          "Entry '" + alias + "' of keystore " + file + " has no certificate chain");
    }
    final List<X509Certificate> certificates = new ArrayList<>(chain.length);
    collectX509(chain, certificates, alias, file);
    return certificates.toArray(X509Certificate[]::new);
  }

  private static void collectX509(
      final Certificate[] chain,
      final List<X509Certificate> sink,
      final String alias,
      final Path file)
      throws KeyStoreException {
    for (final Certificate certificate : chain) {
      if (!(certificate instanceof final X509Certificate x509)) {
        throw new KeyStoreException(
            "Entry '" + alias + "' of keystore " + file + " contains a non-X.509 certificate");
      }
      sink.add(x509);
    }
  }
}
