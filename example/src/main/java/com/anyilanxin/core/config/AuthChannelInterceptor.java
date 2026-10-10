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

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@Slf4j
public class AuthChannelInterceptor implements ChannelInterceptor {

  /**
   * 连接前监听
   *
   * @param message
   * @param channel
   * @return
   */
  @Override
  public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
    final StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor != null) {
      // 1、判断是否首次连接
      if (StompCommand.CONNECT.equals(accessor.getCommand())) {
        final String userId = accessor.getFirstNativeHeader("userId");
        accessor.setUser(() -> userId);
        //            // // 2、注入用户信息
        //            SecurityContext context = SecurityContextHolder.getContext();
        //            Authentication userAuthentication = context.getAuthentication();
        //            if (Objects.isNull(userAuthentication) ||
        // !userAuthentication.isAuthenticated())
        // {
        //                return null;
        //            }
      }
    }

    // 不是首次连接，已经登陆成功
    return message;
  }
}
