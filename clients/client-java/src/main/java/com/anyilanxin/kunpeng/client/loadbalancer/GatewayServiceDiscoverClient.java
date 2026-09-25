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
package com.anyilanxin.kunpeng.client.loadbalancer;

import com.anyilanxin.kunpeng.client.command.gatewaydiscribe.GatewayAddressQueryCommand;
import com.anyilanxin.kunpeng.client.command.gatewaydiscribe.GatewayLoadQueryCommand;

/** 与 kunpeng broker 集群通信的客户端入口。 */
public interface GatewayServiceDiscoverClient extends AutoCloseable {
  /**
   * Request the current cluster topology. Can be used to inspect which brokers are available at
   * which endpoint and which broker is the leader of which partition.
   *
   * <pre>
   * List&#60;BrokerInfo&#62; brokers = kunpengClient
   *  .newTopologyRequest()
   *  .send()
   *  .join()
   *  .getBrokers();
   *
   *  InetSocketAddress address = broker.getSocketAddress();
   *
   *  List&#60;PartitionInfo&#62; partitions = broker.getPartitions();
   * </pre>
   *
   * @return the request where you must call {@code send()}
   */
  GatewayAddressQueryCommand newQueryGatewayRequest();

  /**
   * Query the active-connection count of a specific gateway. The request is issued against whatever
   * gateway this client is connected to; that gateway forwards the request via cluster messaging
   * when {@code targetGrpcAddress} does not match its own address.
   *
   * @param targetGrpcAddress target gateway's gRPC address (host:port form), or empty for "this
   *     gateway"
   * @return the request where you must call {@code send()}
   */
  GatewayLoadQueryCommand newQueryGatewayLoadRequest(String targetGrpcAddress);

  @Override
  void close();
}
