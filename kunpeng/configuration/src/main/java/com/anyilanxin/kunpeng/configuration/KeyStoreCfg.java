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
package com.anyilanxin.kunpeng.configuration;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/** 密钥库配置，包含密钥库文件路径与密码。 */
public final class KeyStoreCfg {
  private File filePath;
  private String password;

  public void init(final String basePath) {
    final var brokerBasePath = Path.of(basePath);
    if (filePath != null) {
      filePath = brokerBasePath.resolve(filePath.toPath()).toFile();
    }
  }

  public File getFilePath() {
    return filePath;
  }

  public KeyStoreCfg setFilePath(final File filePath) {
    this.filePath = filePath;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public KeyStoreCfg setPassword(final String password) {
    this.password = password;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(filePath, password);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final KeyStoreCfg that = (KeyStoreCfg) o;
    return Objects.equals(filePath, that.filePath) && Objects.equals(password, that.password);
  }

  @Override
  public String toString() {
    final var passStr = password == null ? "" : "*****";
    return "GatewayKeyStoreCfg{" + "filePath=" + filePath + ", password='" + passStr + '\'' + '}';
  }
}
