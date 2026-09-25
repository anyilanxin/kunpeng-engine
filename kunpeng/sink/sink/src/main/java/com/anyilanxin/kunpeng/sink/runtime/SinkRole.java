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
package com.anyilanxin.kunpeng.sink.runtime;

/**
 * 一个 {@link SinkService} 实例在其分区中的角色。
 *
 * <p>只有分区 leader 向 Sink 投递记录；follower 只接收 leader 广播的确认位置， 这样 leader 切换后，新 leader 才知道每个 Sink 处理到了哪里。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum SinkRole {
  /** 向 Sink 投递记录，并把确认位置广播给 follower。 */
  LEADER,
  /** 只记录 leader 广播的确认位置；不投递任何记录。 */
  FOLLOWER
}
