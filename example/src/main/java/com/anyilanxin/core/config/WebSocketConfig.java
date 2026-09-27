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
package com.anyilanxin.core.config;

import static com.anyilanxin.core.config.SocketDestinationPrefixes.APP;
import static com.anyilanxin.core.config.SocketDestinationPrefixes.USER;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final AuthChannelInterceptor authChannelInterceptor;
  private final CustomHandshakeHandler customHandshakeHandler;

  /** 消息代理前缀 */
  private static final String[] DESTINATION_PREFIXES =
      new String[] {
        SocketDestinationPrefixes.QUEUE_PROCESS_INSTANCE,
      };

  @Override
  public void registerStompEndpoints(final StompEndpointRegistry registry) {
    registry
        .addEndpoint("/websocket")
        .setAllowedOriginPatterns("*")
        //      .setHandshakeHandler(customHandshakeHandler)
        .withSockJS();
  }

  @Override
  public void configureMessageBroker(final MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes(APP);
    registry.enableSimpleBroker(DESTINATION_PREFIXES);
    registry.setUserDestinationPrefix(USER);
  }

  /**
   * 拦截器方式
   *
   * @param registration
   */
  @Override
  public void configureClientInboundChannel(final ChannelRegistration registration) {
    registration.interceptors(authChannelInterceptor);
  }
}
