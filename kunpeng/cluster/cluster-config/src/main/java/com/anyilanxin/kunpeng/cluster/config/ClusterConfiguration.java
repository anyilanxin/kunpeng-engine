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

/**
 * 集群配置聚合类，同时持有管理配置与 Raft 配置两部分，loadConfiguration 用于以给定配置整体替换当前配置。
 *
 * @author zxuanhong
 * @since
 */
public class ClusterConfiguration {

  private ClusterAdminConfiguration adminConfiguration = ClusterAdminConfiguration.uninitialized();
  private ClusterRaftConfiguration raftConfiguration = ClusterRaftConfiguration.uninitialized();

  private ClusterNodeConfiguration nodeConfiguration = ClusterNodeConfiguration.uninitialized();

  public ClusterAdminConfiguration getAdminConfiguration() {
    return adminConfiguration;
  }

  public void setAdminConfiguration(final ClusterAdminConfiguration adminConfiguration) {
    this.adminConfiguration = adminConfiguration;
  }

  public ClusterRaftConfiguration getRaftConfiguration() {
    return raftConfiguration;
  }

  public void setRaftConfiguration(final ClusterRaftConfiguration raftConfiguration) {
    this.raftConfiguration = raftConfiguration;
  }

  public ClusterNodeConfiguration getNodeConfiguration() {
    return nodeConfiguration;
  }

  public void setNodeConfiguration(final ClusterNodeConfiguration nodeConfiguration) {
    this.nodeConfiguration = nodeConfiguration;
  }

  public ClusterConfiguration loadConfiguration(final ClusterConfiguration clusterConfiguration) {
    adminConfiguration = clusterConfiguration.getAdminConfiguration();
    raftConfiguration = clusterConfiguration.getRaftConfiguration();
    nodeConfiguration = clusterConfiguration.getNodeConfiguration();
    return this;
  }
}
