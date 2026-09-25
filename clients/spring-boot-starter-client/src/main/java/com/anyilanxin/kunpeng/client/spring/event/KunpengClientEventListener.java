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

import com.anyilanxin.kunpeng.client.spring.annotation.processor.KunpengClientLifecycleAware;
import java.util.Set;
import org.springframework.context.event.EventListener;

/**
 * 客户端事件监听器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientEventListener {

  private final Set<KunpengClientLifecycleAware> kunpengClientLifecycleAwareSet;

  public KunpengClientEventListener(
      final Set<KunpengClientLifecycleAware> kunpengClientLifecycleAwareSet) {
    this.kunpengClientLifecycleAwareSet = kunpengClientLifecycleAwareSet;
  }

  @EventListener
  public void handleStart(final KunpengClientCreatedEvent evt) {
    kunpengClientLifecycleAwareSet.forEach(
        kunpengClientLifecycleAware -> kunpengClientLifecycleAware.onStart(evt.getClient()));
  }

  @EventListener
  public void handleStop(final KunpengClientClosingEvent evt) {
    kunpengClientLifecycleAwareSet.forEach(
        kunpengClientLifecycleAware -> kunpengClientLifecycleAware.onStop(evt.getClient()));
  }
}
