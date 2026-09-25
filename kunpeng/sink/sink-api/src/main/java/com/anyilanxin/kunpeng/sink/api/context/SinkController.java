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
package com.anyilanxin.kunpeng.sink.api.context;

import java.time.Duration;
import java.util.Optional;

/**
 * 在 {@code RecordSink#start} 时交给 Sink 的句柄；也是 Sink 向引擎回话的唯一途径。
 *
 * <p>除非特别说明，所有方法均可从任意线程安全调用。
 */
public interface SinkController {

  /**
   * 确认到给定位置（含该位置）的所有记录已处理完成。
   *
   * <p>每个 Sink 的位置必须单调递增——低于当前值的位置会被忽略。
   *
   * @param position 最后一条已可靠写入的记录位置
   */
  void updatePosition(long position);

  /**
   * 确认到给定位置（含该位置）的所有记录已处理完成，并把 {@code metadata} 随位置一并存储。 重启之后可通过 {@link #readMetadata()} 再次读取， Sink
   * 可借此保存内部检查点（例如刷盘水位）。
   *
   * @param position 最后一条已可靠写入的记录位置
   * @param metadata 任意检查点数据，可为 {@code null}
   */
  void updatePosition(long position, byte[] metadata);

  /**
   * @return 本 Sink 目前已确认到的位置
   */
  long getPosition();

  /**
   * 在 {@code delay} 之后执行一次 {@code task}。定时器只在 Sink 打开期间有效；关闭时未执行的任务会被丢弃。
   *
   * @param delay 执行任务前等待的时长；必须为正
   * @param task 在 Sink 线程上执行的动作
   * @return 用于取消任务的句柄
   */
  CancellableTask scheduleTask(Duration delay, Runnable task);

  /**
   * 读取最后一次 {@link #updatePosition(long, byte[])} 存储的元数据，重启后会自动恢复。
   *
   * @return 存储的元数据；从未存储过则为空
   */
  Optional<byte[]> readMetadata();
}
