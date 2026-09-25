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
package com.anyilanxin.kunpeng.client.spring.actuator;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.topology.TopologyCommandResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;

/**
 * 客户端健康检查指示器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientHealthIndicator extends AbstractHealthIndicator {

  private final KunpengClient client;

  @Autowired
  public KunpengClientHealthIndicator(final KunpengClient client) {
    this.client = client;
  }

  @Override
  protected void doHealthCheck(final Health.Builder builder) {
    final TopologyCommandResponse topology = client.newTopologyCommand().send().join();
    if (topology.getBrokers().isEmpty()) {
      builder.down();
    } else {
      builder.up();
    }
  }
}
