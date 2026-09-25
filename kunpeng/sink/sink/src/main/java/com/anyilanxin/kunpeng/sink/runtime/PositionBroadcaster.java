/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.sink.runtime;

import com.anyilanxin.kunpeng.cluster.config.messaging.PartitionMessagingService;
import com.anyilanxin.kunpeng.cluster.utils.logging.ThrottledLogger;
import com.anyilanxin.kunpeng.sink.SinkLoggers;
import com.anyilanxin.kunpeng.sink.protocol.SinkPositionsMessage;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;

/**
 * 通过分区的消息服务交换 {@link SinkPositionsMessage}：leader 广播确认位置， follower 应用这些位置，这样 leader 切换后新 leader 能从旧
 * leader 停下的地方继续。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class PositionBroadcaster implements AutoCloseable {

  private static final Logger LOG = SinkLoggers.SINK;
  private static final Logger PERIODIC_LOG =
      new ThrottledLogger(SinkLoggers.SINK, Duration.ofSeconds(60));

  private final PartitionMessagingService messaging;
  private final String topic;
  private final BiConsumer<String, SinkPositionsMessage.SinkPosition> positionConsumer;

  public PositionBroadcaster(
      final BiConsumer<String, SinkPositionsMessage.SinkPosition> positionConsumer,
      final PartitionMessagingService messaging,
      final String topic) {
    this.positionConsumer = positionConsumer;
    this.messaging = messaging;
    this.topic = topic;
  }

  /** 开始消费广播位置；收到的消息在 {@code executor} 上应用。 */
  public void subscribe(final Executor executor) {
    messaging.subscribe(topic, this::applyBroadcast, executor);
  }

  private void applyBroadcast(final ByteBuffer payload) {
    final var message = new SinkPositionsMessage();
    message.wrap(new UnsafeBuffer(payload), 0, payload.remaining());

    LOG.trace("Received sink positions {}", message.getPositions());
    PERIODIC_LOG.debug("Current received sink positions {}", message.getPositions());
    message.getPositions().forEach(positionConsumer);
  }

  /**
   * 发布一份全部 Sink 位置的快照。
   *
   * @param message 待广播的、已填充好的消息
   */
  public void broadcast(final SinkPositionsMessage message) {
    messaging.broadcast(topic, message.toByteBuffer());
  }

  @Override
  public void close() {
    messaging.unsubscribe(topic);
  }
}
