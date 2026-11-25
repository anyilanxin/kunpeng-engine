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
package com.anyilanxin.kunpeng.cluster.config.topology.cluster;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;

/**
 * 分区当前 leader 的一次读出结果：成员、任期与业务面地址取自同一份拓扑视图，三者不会错位。
 *
 * <p>任期参与 leader 身份的比较：同一成员重启后重新当选时成员不变而任期递增，重启后的节点已不记得此前收下的请求。
 *
 * @param memberId leader 所在成员
 * @param term leader 任期
 * @param businessAddress leader 的业务面地址；业务端口尚未广播时为 {@code null}
 * @author zxuanhong
 * @since 2026.9.0
 */
public record PartitionLeaderInfo(MemberId memberId, long term, Address businessAddress) {}
