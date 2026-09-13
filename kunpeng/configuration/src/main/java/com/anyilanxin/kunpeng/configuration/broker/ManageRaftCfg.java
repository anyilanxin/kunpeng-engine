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

import static com.anyilanxin.kunpeng.configuration.broker.partition.PartitioningRaftConfig.DEFAULT_REPLICATION_FACTOR;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * 管理 raft 分区配置
 *
 * @author zxuanhong
 * @since
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ManageRaftCfg extends RaftCfg {
  /** 副本数量 */
  private int replicationFactor = DEFAULT_REPLICATION_FACTOR;

  /** 初始引导节点 */
  private String initBrokerId;

  @Override
  public void init(final BrokerCfg globalConfig, final String brokerBase) {
    super.init(globalConfig, brokerBase);
    if (StringUtils.isBlank(initBrokerId)) {
      throw new RuntimeException("init broker id is empty");
    }
  }
}
