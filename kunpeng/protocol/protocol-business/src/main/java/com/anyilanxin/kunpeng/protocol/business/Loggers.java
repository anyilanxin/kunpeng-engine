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
package com.anyilanxin.kunpeng.protocol.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Loggers {
  public static final Logger CLUSTERING_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.clustering");
  public static final Logger SYSTEM_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.system");
  public static final Logger SYSTEM_PROCESS_STATE_MACHINE_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.state.machine.processing");
  public static final Logger SYSTEM_REPOSITOR_STATE_MACHINE_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.state.machine.repositor");
  public static final Logger TRANSPORT_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.transport");
  public static final Logger PROCESS_REPOSITORY_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.process.repository");
  public static final Logger LOGSTREAMS_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.logstreams");

  public static final Logger RAFT = LoggerFactory.getLogger("com.anyilanxin.kunpeng.broker.raft");
  public static final Logger SNAPSHOT_LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.logstreams.snapshot");
}
