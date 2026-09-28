/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.cluster.messaging.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 探针：向一个确定无人订阅的主题广播时，丢弃要留痕（此前为静默空操作，事件链路断流无法定位）； 留痕按分钟节流，网关未订阅期间的高频广播不会刷屏。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("无订阅主题的广播日志")
class NoSubscriberBroadcastLoggingTest {

  private static final String ORPHAN_TOPIC = "topic-with-no-subscriber-anywhere";

  private final CapturingAppender appender = new CapturingAppender();
  private final Logger logger = (Logger) LogManager.getLogger(DefaultClusterEventService.class);

  @AfterEach
  void tearDown() {
    logger.removeAppender(appender);
    appender.stop();
  }

  @Test
  void broadcastToUnsubscribedTopicLeavesTraceAndThrottles() {
    final var membership = mock(ClusterMembershipService.class);
    when(membership.getLocalMember())
        .thenReturn(Member.builder("1").withHost("localhost").withPort(5000).build());
    final var messaging =
        new TestMessagingServiceFactory().newMessagingService(Address.from("localhost", 5000));
    final var service = new DefaultClusterEventService(membership, messaging);

    logger.addAppender(appender);
    appender.start();

    service.broadcast(ORPHAN_TOPIC, "ping");
    assertThat(appender.messages)
        .anyMatch(message -> message.contains("无可用订阅者") && message.contains(ORPHAN_TOPIC));

    // 节流窗口内的后续丢弃不再重复打印
    appender.messages.clear();
    service.broadcast(ORPHAN_TOPIC, "ping-again");
    service.broadcast(ORPHAN_TOPIC, "ping-more");
    assertThat(appender.messages).isEmpty();
  }

  /** 捕获 DefaultClusterEventService 名下 logger 的全部输出（log4j2 测试后端）。 */
  private static final class CapturingAppender extends AbstractAppender {
    private final List<String> messages = new CopyOnWriteArrayList<>();

    CapturingAppender() {
      super("no-subscriber-probe", null, null, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(final LogEvent event) {
      messages.add(event.getMessage().getFormattedMessage());
    }
  }
}
