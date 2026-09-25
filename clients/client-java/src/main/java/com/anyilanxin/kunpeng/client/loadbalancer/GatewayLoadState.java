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

/**
 * Mutable per-gateway snapshot populated by the load-poll cycle. Defaults are optimistic (zero
 * connections, online) so a freshly discovered gateway is selectable before its first load query
 * completes.
 */
final class GatewayLoadState {
  volatile int activeConnections = 0;
  volatile boolean online = true;
}
