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
package com.anyilanxin.kunpeng.protocol.common;

/**
 * @author zxuanhong
 * @since
 */
public interface ClusterCommonConstant {
  String ADMIN_RAFT_GROUP = "admin-partition";
  String BUSINESS_RAFT_GROUP = "raft-partition";
  String RUNTIME_DIRECTORY = "runtime";
  String PARTITIONS_DIRECTORY = "partitions";
  String CLUSTER_DISPATCH_TOPIC_ACK = "CLUSTER_DISPATCH_ACK";
  String CLUSTER_NODE_SOURCE_TOPIC = "CLUSTER_NODE_SOURCE";
  String TOPOLOGY_PROPERTY_KEY = "cluster.topology.partitions";
  String NODE_SOURCE_PROPERTY_KEY = "cluster.topology.nodesource";

  int INITIAL_NODE_SOURCE = 0;
  int INITIAL_PARTITION_SOURCE = 0;
  String MANAGE_RAFT = ADMIN_RAFT_GROUP + "-1";
  String BUSINESS_PARTITION_ONE_RAFT = BUSINESS_RAFT_GROUP + "-1";

  int ADMIN_PARTITION_SOURCE = 1;
  int BUSINESS_RAFT_ONE_SOURCE = INITIAL_PARTITION_SOURCE + 2;

  String NODE_SOURCE_TOPIC = "NODE-SOURCE";

  String PARTITION_SOURCE_KEY = "SOURCE_ID";
  String PARTITION_AGENT_SOURCE_KEY = "AGENT_SOURCE_IDS";
}
