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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.ConstructableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceiveSnapshotStore;

/**
 * raft 分区镜像存储门面：一个分区既能本地拍摄（constructable）又能接收 install/传输 （receive），二者共享同一份持久存储（同一目录、同一最新镜像状态）。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface RaftSnapshotStore extends ConstructableSnapshotStore, ReceiveSnapshotStore {}
