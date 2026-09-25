/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.jobstream;

import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamCoordinator;

/**
 * job 流推送服务件：协调器（订阅投影 + 轮转选流 + 推送应答）、引擎适配器与失败回退处理器。
 *
 * <p>注册在 broker 层（不依赖 raft）；业务分区 transition 步骤消费 {@link CoordinatorJobStreamer} 注入引擎、 消费 {@link
 * PushFailureFallback} 在 leader 期登记分区写入器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public record JobStreamDispatcher(
    JobStreamCoordinator coordinator,
    CoordinatorJobStreamer streamer,
    PushFailureFallback fallbackHandler) {}
