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

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * @author zxuanhong
 * @date 2026-04-23 09:30
 * @since
 */
@Component
public class CustomHandshakeHandler extends DefaultHandshakeHandler {
  @Override
  protected Principal determineUser(
      final ServerHttpRequest request,
      final WebSocketHandler wsHandler,
      final Map<String, Object> attributes) {
    // 从前端参数获取 userId
    final String userId = request.getHeaders().getFirst("userId");
    return new Principal() {
      @Override
      public String getName() {
        return userId;
      }
    };
  }
}
