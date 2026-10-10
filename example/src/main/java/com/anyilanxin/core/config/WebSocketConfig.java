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
