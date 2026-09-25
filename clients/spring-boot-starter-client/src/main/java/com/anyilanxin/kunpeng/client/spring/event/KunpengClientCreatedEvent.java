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
 * Event which is triggered when the kunpengClient was created. This can be used to register further
 * work that should be done, like starting job workers or doing deployments.
 *
 * <p>In a normal production application this event is simply fired once during startup when the
 * kunpeng client 创建完成、可开始使用. However, in test cases it might be fired multiple times, as every test
 * case gets its own dedicated引擎 also leading to new kunpengClients being created (at least
 * logically, as the kunpengClient Spring bean might simply be a proxy always pointing to the right
 * client automatically to avoid problems with @Autowire).
 *
 * <p>Furthermore, when `camunda.client.enabled=false`, the event might not be fired ever
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientCreatedEvent extends ApplicationEvent {

  public final KunpengClient client;

  public KunpengClientCreatedEvent(final Object source, final KunpengClient client) {
    super(source);
    this.client = client;
  }

  public KunpengClient getClient() {
    return client;
  }
}
