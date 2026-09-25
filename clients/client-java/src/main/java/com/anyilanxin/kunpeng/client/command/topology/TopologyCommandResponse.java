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
package com.anyilanxin.kunpeng.client.command.topology;

import com.anyilanxin.kunpeng.client.command.BrokerInfo;
import java.util.List;

public interface TopologyCommandResponse {
  /**
   * @return all (known) brokers of the cluster
   */
  List<BrokerInfo> getBrokers();

  /**
   * @return 集群中的 broker 数量
   */
  int getClusterSize();

  /**
   * @return the configured number of partitions
   */
  int getPartitionsCount();

  /**
   * @return the configured replication factor for every partition
   */
  int getReplicationFactor();

  /**
   * @return the gateway version or an empty string if none was found
   */
  String getGatewayVersion();
}
