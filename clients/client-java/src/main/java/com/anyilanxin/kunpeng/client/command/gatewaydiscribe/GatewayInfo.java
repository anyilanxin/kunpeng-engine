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
package com.anyilanxin.kunpeng.client.command.gatewaydiscribe;

public interface GatewayInfo {
  /**
   * @return the node if of the broker
   */
  String getNodeId();

  /**
   * @return the address host of the broker
   */
  String getGatewayAddress();

  /**
   * @return the address port of the broker
   */
  String getGrpcAddress();

  /**
   * @return whether the gateway allows client discovery
   */
  boolean isAllowClientDiscovery();

  /**
   * @return the version of the broker
   */
  String getVersion();
}
