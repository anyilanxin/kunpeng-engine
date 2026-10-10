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
package com.anyilanxin.core.config;

/**
 * 授权类型
 *
 * @author zxh
 * @date 2020-09-12 10:52
 * @since 1.0.0
 */
public interface SocketDestinationPrefixes {
  String QUEUE = "/queue";
  String TOPIC = "/topic";
  String USER = "/user";
  String APP = "/app";

  /** 流程实例 */
  String QUEUE_PROCESS_INSTANCE = QUEUE + "/process-instance";
}
