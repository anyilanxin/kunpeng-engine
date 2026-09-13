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
package com.anyilanxin.kunpeng.configuration.broker;

import com.anyilanxin.kunpeng.configuration.gateway.GatewayCfg;

/** 内嵌网关配置，在 broker 内启用并配置嵌入式 gateway。 */
public final class EmbeddedGatewayCfg extends GatewayCfg implements ConfigurationEntry {
  private boolean enable = true;

  @Override
  public void init(final BrokerCfg globalConfig, final String brokerBase) {
    super.init(brokerBase);
  }

  public boolean isEnable() {
    return enable;
  }

  public EmbeddedGatewayCfg setEnable(final boolean enable) {
    this.enable = enable;
    return this;
  }
}
