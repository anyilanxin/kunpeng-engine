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
package com.anyilanxin.kunpeng.client.spring.event;

import com.anyilanxin.kunpeng.client.KunpengClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;

/**
 * 客户端生命周期事件发布器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengLifecycleEventProducer implements SmartLifecycle {

  protected boolean running = false;

  private final ApplicationEventPublisher publisher;

  private final KunpengClient client;

  public KunpengLifecycleEventProducer(
      final KunpengClient client, final ApplicationEventPublisher publisher) {
    this.client = client;
    this.publisher = publisher;
  }

  @Override
  public void start() {
    publisher.publishEvent(new KunpengClientCreatedEvent(this, client));
    running = true;
  }

  @Override
  public void stop() {
    publisher.publishEvent(new KunpengClientClosingEvent(this, client));
    running = false;
  }

  @Override
  public boolean isRunning() {
    return running;
  }
}
