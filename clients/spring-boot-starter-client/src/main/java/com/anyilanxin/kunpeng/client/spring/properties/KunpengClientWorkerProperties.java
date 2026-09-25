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

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * worker 配置属性。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientWorkerProperties {
  @NestedConfigurationProperty
  private KunpengClientJobWorkerProperties defaults = new KunpengClientJobWorkerProperties(true);

  @NestedConfigurationProperty
  private Map<String, KunpengClientJobWorkerProperties> override = new HashMap<>();

  public KunpengClientJobWorkerProperties getDefaults() {
    return defaults;
  }

  public void setDefaults(final KunpengClientJobWorkerProperties defaults) {
    this.defaults = defaults;
  }

  public Map<String, KunpengClientJobWorkerProperties> getOverride() {
    return override;
  }

  public void setOverride(final Map<String, KunpengClientJobWorkerProperties> override) {
    this.override = override;
  }

  @Override
  public String toString() {
    return "KunpengClientWorkerProperties{"
        + "defaults="
        + defaults
        + ", override="
        + override
        + '}';
  }
}
