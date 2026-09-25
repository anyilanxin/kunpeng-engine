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
package com.anyilanxin.kunpeng.client.spring.properties;

import java.nio.file.Path;

/**
 * 客户端认证断言配置属性。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientAuthClientAssertionProperties {
  /** The path to the keystore where the client assertion certificate is stored. */
  private Path keystorePath;

  /** The password of the referenced keystore. */
  private String keystorePassword;

  /**
   * The alias of the key containing the certificate used to sign the client assertion certificate.
   * If not set, the first alias from the keystore is used.
   */
  private String keystoreKeyAlias;

  /** The password of the key referenced by the alias. If not set, the keystore password is used. */
  private String keystoreKeyPassword;

  public Path getKeystorePath() {
    return keystorePath;
  }

  public void setKeystorePath(final Path keystorePath) {
    this.keystorePath = keystorePath;
  }

  public String getKeystorePassword() {
    return keystorePassword;
  }

  public void setKeystorePassword(final String keystorePassword) {
    this.keystorePassword = keystorePassword;
  }

  public String getKeystoreKeyAlias() {
    return keystoreKeyAlias;
  }

  public void setKeystoreKeyAlias(final String keystoreKeyAlias) {
    this.keystoreKeyAlias = keystoreKeyAlias;
  }

  public String getKeystoreKeyPassword() {
    return keystoreKeyPassword;
  }

  public void setKeystoreKeyPassword(final String keystoreKeyPassword) {
    this.keystoreKeyPassword = keystoreKeyPassword;
  }
}
