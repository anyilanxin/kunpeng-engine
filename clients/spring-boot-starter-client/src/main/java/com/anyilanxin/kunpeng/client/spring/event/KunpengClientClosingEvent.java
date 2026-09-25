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
import org.springframework.context.ApplicationEvent;

/**
 * Emitted when the kunpengClient is about to close. Typically, during application shutdown, but
 * maybe more often in test case or never if the kunpengClient is disabled, see {@link
 * KunpengClientCreatedEvent} for more details
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientClosingEvent extends ApplicationEvent {

  public final KunpengClient client;

  public KunpengClientClosingEvent(final Object source, final KunpengClient client) {
    super(source);
    this.client = client;
  }

  public KunpengClient getClient() {
    return client;
  }
}
