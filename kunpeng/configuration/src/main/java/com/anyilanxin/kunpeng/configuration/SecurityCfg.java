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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** 安全配置，包含是否启用 TLS、证书链路径、私钥路径与密钥库配置。 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public final class SecurityCfg {
  private static final boolean DEFAULT_ENABLED = false;

  private boolean enabled = DEFAULT_ENABLED;
  private File certificateChainPath;
  private File privateKeyPath;
  private final KeyStoreCfg keyStore = new KeyStoreCfg();

  public void init(final String basePath) {
    keyStore.init(basePath);
  }
}
