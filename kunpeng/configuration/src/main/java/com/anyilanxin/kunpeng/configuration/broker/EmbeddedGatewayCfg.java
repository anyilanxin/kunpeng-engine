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
import lombok.Getter;
import lombok.Setter;

/**
 * 内嵌网关配置，在 broker 内启用并配置嵌入式 gateway。整套网关参数绑定于 {@code kunpeng.broker.gateway.*}，与独立网关的 {@code
 * kunpeng.gateway.*} 互不影响（同机双形态部署不会配置冲突）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Getter
@Setter
public final class EmbeddedGatewayCfg extends GatewayCfg implements ConfigurationEntry {
  private boolean enable = true;

  @Override
  public void init(final BrokerCfg globalConfig, final String brokerBase) {
    super.init(brokerBase);
  }
}
