/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.protocol.common;

/**
 * 集群公共常量定义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ClusterCommonConstant {
  String ADMIN_RAFT_GROUP = "admin-partition";
  String BUSINESS_RAFT_GROUP = "raft-partition";
  String RUNTIME_DIRECTORY = "runtime";
  String PARTITIONS_DIRECTORY = "partitions";
  String CLUSTER_DISPATCH_TOPIC_ACK = "CLUSTER_DISPATCH_ACK";
  String CLUSTER_NODE_SOURCE_TOPIC = "CLUSTER_NODE_SOURCE";
  String NODE_SOURCE_PROPERTY_KEY = "cluster.topology.nodesource";
  String INNER_PARTITION_TOPIC_PREFIX = "inter-partition-";

  /** SWIM 成员业务实体的属性键（一个实体一个键，Base64(SBE 帧) 为值） */
  String BROKER_INFO_PROPERTY = "brokerInfo";

  String JOB_SUBSCRIPTION_INFO_PROPERTY = "jobSubscriptionInfo";

  String GATEWAY_INFO_PROPERTY = "gatewayInfo";

  int INITIAL_NODE_SOURCE = 0;
  int INITIAL_PARTITION_SOURCE = 1;
  String MANAGE_RAFT = ADMIN_RAFT_GROUP + "-1";
  String BUSINESS_PARTITION_ONE_RAFT = BUSINESS_RAFT_GROUP + "-1";

  int ADMIN_PARTITION_SOURCE = 2;
  int BUSINESS_RAFT_GLOBAL_SOURCE = INITIAL_PARTITION_SOURCE;

  String NODE_SOURCE_TOPIC = "NODE-SOURCE";

  String PARTITION_SOURCE_KEY = "SOURCE_ID";
  String PARTITION_AGENT_SOURCE_KEY = "AGENT_SOURCE_IDS";
  String PROCESS_POSITION = "processPosition";
  String SNAPSHOT_TIME = "snapshotTimeMillis";
  String SINK_POSITION = "sinkPosition";
  String COMPACTION_POSITION = "compactionPosition";
}
