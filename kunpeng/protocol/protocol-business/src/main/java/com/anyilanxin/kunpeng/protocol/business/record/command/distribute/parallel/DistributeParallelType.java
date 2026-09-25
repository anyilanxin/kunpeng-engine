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
package com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum DistributeParallelType {
  /** 不是分发 */
  NOT_DISTRIBUTE(0),

  /** 分发数据 */
  DISTRIBUTE(1),

  /** 分发后处理 */
  DISTRIBUTE_AFTER(2),
  ;

  private final short value;

  DistributeParallelType(final int value) {
    this.value = (short) value;
  }

  public short getValueState() {
    return value;
  }
}
