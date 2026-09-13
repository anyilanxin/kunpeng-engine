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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistableSnapshot;

/**
 * 拍摄式 pending 镜像：由 {@link ConstructableSnapshotStore#newTransientSnapshot} 创建， 创建过程已按次传入的
 * {@link SnapshotContentWriter} 完成内容拍摄。 标记接口——区分"拍摄产生"的 pending 镜像与其他来源（如接收），无自有方法。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface ConstructableSnapshot extends PersistableSnapshot {}
