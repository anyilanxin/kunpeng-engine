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
package com.anyilanxin.kunpeng.client.command.job.worker;

/**
 * Represents an active job worker that performs jobs of a certain type. While a registration is
 * open, the client continuously receives jobs from the broker and hands them to a registered {@link
 * JobHandler}.
 */
public interface JobWorker extends AutoCloseable {
  /**
   * @return true if this registration is currently active and work items are being received for it
   */
  boolean isOpen();

  /**
   * @return true if this registration is not open and is not in the process of opening or closing
   */
  boolean isClosed();

  /**
   * Closes this registration and stops receiving new work items. Blocks until all previously
   * received items have been handed to the worker.
   */
  @Override
  void close();
}
