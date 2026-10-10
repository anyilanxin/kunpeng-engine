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
package com.anyilanxin.core;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * @author zxuanhong
 * @date 2026-04-23 15:14
 * @since
 */
@Component
@RequiredArgsConstructor
public class WebsocketEventAdapt {
  private final ApplicationEventPublisher eventPublisher;
  private final Map<String, String> topicInfo = new ConcurrentHashMap<>();

  @EventListener
  public void handleUnsubscribe(final SessionUnsubscribeEvent event) {
    final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    final String subscriptionId = accessor.getSubscriptionId();
    final String topic = topicInfo.remove(subscriptionId);
    final Principal user = event.getUser();
    final UnsubscribeEvent unsubscribeEvent = new UnsubscribeEvent();
    unsubscribeEvent.setUser(user);
    unsubscribeEvent.setTopic(topic);
    unsubscribeEvent.setSubscriptionId(subscriptionId);
    unsubscribeEvent.setSessionId(accessor.getSessionId());
    unsubscribeEvent.setAccessor(accessor);
    unsubscribeEvent.setSimpleTopic("");
    if (StringUtils.isNotBlank(topic)) {
      unsubscribeEvent.setParam(topic.substring(topic.lastIndexOf("/") + 1));
      unsubscribeEvent.setSimpleTopic(topic.substring(0, topic.lastIndexOf("/")));
    }
    eventPublisher.publishEvent(unsubscribeEvent);
  }

  @EventListener
  public void handleSubscribe(final SessionSubscribeEvent event) {
    final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    final String subscriptionId = accessor.getSubscriptionId();
    final String topic = accessor.getDestination();
    topicInfo.put(subscriptionId, topic);
    final Principal user = event.getUser();
    final SubscribeEvent subscribeEvent = new SubscribeEvent();
    subscribeEvent.setUser(user);
    subscribeEvent.setTopic(topic);
    subscribeEvent.setSubscriptionId(subscriptionId);
    subscribeEvent.setSessionId(accessor.getSessionId());
    subscribeEvent.setAccessor(accessor);
    subscribeEvent.setSimpleTopic("");
    if (StringUtils.isNotBlank(topic)) {
      subscribeEvent.setParam(topic.substring(topic.lastIndexOf("/") + 1));
      subscribeEvent.setSimpleTopic(topic.substring(0, topic.lastIndexOf("/")));
    }
    eventPublisher.publishEvent(subscribeEvent);
  }
}
