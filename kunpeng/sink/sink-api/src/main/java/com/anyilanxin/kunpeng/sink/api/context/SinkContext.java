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

import io.micrometer.core.instrument.MeterRegistry;
import java.time.InstantSource;
import org.slf4j.Logger;

/** 单个 {@code RecordSink} 实例的环境上下文。 */
public interface SinkContext {

  /**
   * @return 作用域限定到本 Sink 的指标注册表，用于自定义指标
   */
  MeterRegistry getMeterRegistry();

  /**
   * @return 已按本 Sink 命名的日志器
   */
  Logger getLogger();

  /** 引擎的时间源；请优先使用它而不是 {@link System#currentTimeMillis()}，便于测试控制时间。 */
  InstantSource getClock();

  /**
   * @return 本 Sink 的配置 id 与原始参数
   */
  SinkConfiguration getConfiguration();

  /** 该 Sink 实例绑定的分区。在配置校验阶段（Sink 尚未真正安装时），此值为 {@link #PARTITION_ID_UNSET}。 */
  int getPartitionId();

  /** 配置校验阶段（尚未绑定分区）使用的分区 id。 */
  int PARTITION_ID_UNSET = Integer.MIN_VALUE;

  /**
   * 限制哪些记录能到达 {@code RecordSink#sink}；被匹配器拒绝的记录由引擎直接确认，不再投递。
   *
   * @param matcher 待安装的匹配器；允许替换已有匹配器
   */
  void setRecordMatcher(RecordMatcher matcher);
}
