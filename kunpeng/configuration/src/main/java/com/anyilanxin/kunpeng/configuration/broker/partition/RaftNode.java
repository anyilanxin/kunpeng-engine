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
package com.anyilanxin.kunpeng.configuration.broker.partition;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * raft 节点配置，包含节点 ID 与优先级。
 *
 * @author zxuanhong
 * @since
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class RaftNode {
  /** 默认节点 ID 为 0，取自开发中常用的单节点部署方式。 */
  private static final String DEFAULT_NODE_ID = "node0";

  /** 默认优先级为 1。未启用优先级选举时，该值对系统无影响，可保持默认。若启用优先级选举，所有节点 优先级均可为 1，但此时选举本质上等同于无优先级的普通选举。 */
  private static final int DEFAULT_PRIORITY = 1;

  private String nodeId = DEFAULT_NODE_ID;

  private int priority = DEFAULT_PRIORITY;
}
