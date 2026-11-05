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
package com.anyilanxin.kunpeng.client.command.usertask;

/** 加签模式：并行=全量同开；串行=按名单顺序一次一个。 */
public enum AddSignMode {
  /** 并行：名单全量同时开任务 */
  PARALLEL,
  /** 串行：按名单顺序依次开任务，上一位完成开下一位 */
  SEQUENTIAL
}
