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
public class UnsubscribeEvent implements Serializable {
  @Serial private static final long serialVersionUID = 1776928407099L;

  private String subscriptionId;
  private String sessionId;
  private String topic;
  private String param;
  private String simpleTopic;
  private Principal user;
  private StompHeaderAccessor accessor;
}
