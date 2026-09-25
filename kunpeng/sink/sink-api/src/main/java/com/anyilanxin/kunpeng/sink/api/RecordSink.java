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
package com.anyilanxin.kunpeng.sink.api;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.sink.api.context.SinkContext;
import com.anyilanxin.kunpeng.sink.api.context.SinkController;

/**
 * 引擎记录的对外消费 SPI。
 *
 * <p>实现类由引擎发现并实例化：要么在 broker 配置中注册，要么从外部 jar 加载。因此每个实现类都必须提供
 * 公有的无参构造器；除构造器之外需要的一切，都应通过生命周期回调获取，而不是在构造器里完成。
 *
 * <p>生命周期概览：
 *
 * <ol>
 *   <li>{@link #initialize(SinkContext)} - 调用一次，校验并应用配置；此处抛出异常会阻止引擎启动（或该 Sink 被启用）。
 *   <li>{@link #start(SinkController)} - 第一条记录进入 {@link #sink(BusinessEventRecord)} 之前调用一次；在此分配资源。
 *   <li>{@link #sink(BusinessEventRecord)} - 按日志顺序对每一条匹配的记录调用；期间投递可被 {@link #pause()} 暂停、{@link
 *       #resume()} 恢复，可反复多次。
 *   <li>{@link #close()} - 关闭时调用一次；在此释放资源。
 * </ol>
 *
 * <p>记录确认：记录被可靠处理之后，调用 {@link SinkController#updatePosition(long)}
 * （或带元数据的重载）进行确认。重启后记录会重投递至最后确认的位置为止，因此从不确认的实现也会阻塞日志压缩。
 */
public interface RecordSink {

  /**
   * 初始化并校验配置，先于 Sink 启动。
   *
   * <p>此处抛出异常会使该 Sink 的启动（或启用操作）失败，因此这里是检查必填配置是否缺失的正确位置。 重型资源请留到 {@link #start(SinkController)}
   * 再分配。
   *
   * @param context 该 Sink 环境的只读视图
   */
  default void initialize(final SinkContext context) throws Exception {}

  /**
   * 为接收记录做准备工作。
   *
   * @param controller 用于确认记录、读取元数据和调度任务的句柄
   */
  default void start(final SinkController controller) {}

  /**
   * 消费一条记录。
   *
   * <p>记录按日志顺序、一次一条地到达。方法正常返回即视为成功；抛出异常时，同一条记录会以递增的退避时间重试， 直到成功或服务关闭。因此实现应显式处理错误，只让真正瞬时的异常逃逸出去。
   *
   * <p>传入的 record 包装的是引擎复用的内存，下一条记录会覆盖它。若要在本次调用之后继续持有记录， 请通过 {@link Record#copyOf()} 取副本。
   *
   * @param record 待移交的日志条目
   */
  void sink(final BusinessEventRecord<?> record);

  /** 释放资源；在 Sink 生命周期结束时仅调用一次。 */
  default void close() {}

  /**
   * 记录投递被暂停时调用一次（引擎级暂停，软暂停不触发本回调）。
   *
   * <p>此后不会再有 {@link #sink(BusinessEventRecord)} 调用，直到 {@link #resume()}。实现可在此刷出
   * 缓冲、释放独占连接等；抛出异常会使暂停操作失败。
   */
  default void pause() {}

  /**
   * 暂停后恢复投递时调用一次，先于下一条记录的 {@link #sink(BusinessEventRecord)}。
   *
   * <p>实现可在此重建 {@link #pause()} 期间释放的资源；抛出异常会使恢复操作失败。
   */
  default void resume() {}

  /** 删除该 Sink 到目前为止写出的全部数据；由集群级清理操作触发。必须幂等，且可能被重试。 */
  default void purge() throws Exception {}

  /** 当该 Sink 实例所属的分区从集群移除时调用。实现通常在此清理仅属于该分区的数据。 */
  default void onPartitionRemoved() throws Exception {}
}
