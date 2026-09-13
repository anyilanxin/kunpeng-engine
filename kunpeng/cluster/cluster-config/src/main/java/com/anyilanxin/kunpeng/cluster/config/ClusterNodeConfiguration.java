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
package com.anyilanxin.kunpeng.cluster.config;

import java.io.Serial;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 管理分区（admin partition）的配置信息，记录配置版本、是否本地管理模式、管理分区元数据以及当前节点是否为集群组建发起者。
 *
 * @author zxuanhong
 * @since
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class ClusterNodeConfiguration implements Serializable {
  @Serial private static final long serialVersionUID = 1788230919972L;

  private int nodeUniqueId;

  private int version;

  public boolean isUninitialized() {
    return version <= 0;
  }

  public static ClusterNodeConfiguration uninitialized() {
    return new ClusterNodeConfiguration();
  }

  @Override
  public ClusterNodeConfiguration clone() {
    final ClusterNodeConfiguration clone = new ClusterNodeConfiguration();
    clone.nodeUniqueId = nodeUniqueId;
    clone.version = version;
    return clone;
  }
}
