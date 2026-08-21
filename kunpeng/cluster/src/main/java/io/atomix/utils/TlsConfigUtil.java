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
package io.atomix.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import java.util.List;

/** Utilities to extract TLS material from PKCS#12 key stores. */
public final class TlsConfigUtil {

  private static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";

  private TlsConfigUtil() {}

  /**
   * Reads the certificate chain from the given key store.
   *
   * @param keyStore the key store file, in PKCS#12 format
   * @param keyStorePassword the key store password; may be {@code null}
   * @return the certificate chain
   */
  public static X509Certificate[] getCertificateChain(final File keyStore, final String keyStorePassword)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    final var ks = loadKeyStore(keyStore.toPath(), keyStorePassword);
    final List<X509Certificate> chain = new ArrayList<>();
    final var aliases = ks.aliases();
    while (aliases.hasMoreElements()) {
      final var alias = aliases.nextElement();
      if (ks.isKeyEntry(alias)) {
        for (final Certificate certificate : ks.getCertificateChain(alias)) {
          chain.add((X509Certificate) certificate);
        }
        break;
      }
    }
    if (chain.isEmpty()) {
      throw new KeyStoreException("No key entry with certificate chain found in " + keyStore);
    }
    return chain.toArray(X509Certificate[]::new);
  }

  /**
   * Reads the private key from the given key store.
   *
   * @param keyStore the key store file, in PKCS#12 format
   * @param keyStorePassword the key store password; may be {@code null}
   * @return the private key
   */
  public static PrivateKey getPrivateKey(final File keyStore, final String keyStorePassword)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException,
          UnrecoverableKeyException {
    final var ks = loadKeyStore(keyStore.toPath(), keyStorePassword);
    final char[] password = keyStorePassword == null ? null : keyStorePassword.toCharArray();
    final var aliases = ks.aliases();
    while (aliases.hasMoreElements()) {
      final var alias = aliases.nextElement();
      if (ks.isKeyEntry(alias)) {
        final var key = ks.getKey(alias, password);
        if (key instanceof final PrivateKey privateKey) {
          return privateKey;
        }
      }
    }
    throw new KeyStoreException("No private key found in " + keyStore);
  }

  private static KeyStore loadKeyStore(final Path path, final String password)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    final var ks = KeyStore.getInstance(KEYSTORE_TYPE_PKCS12);
    try (final var input = new FileInputStream(path.toFile())) {
      ks.load(input, password == null ? null : password.toCharArray());
    }
    return ks;
  }
}
