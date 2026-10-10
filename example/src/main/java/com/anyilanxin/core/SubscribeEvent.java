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

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

/**
 * @author zxuanhong
 * @date 2026-04-23 15:11
 * @since
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class SubscribeEvent implements Serializable {
  @Serial private static final long serialVersionUID = 1776928378044L;

  private String subscriptionId;
  private String sessionId;
  private String topic;
  private String simpleTopic;
  private String param;
  private Principal user;
  private StompHeaderAccessor accessor;
}
