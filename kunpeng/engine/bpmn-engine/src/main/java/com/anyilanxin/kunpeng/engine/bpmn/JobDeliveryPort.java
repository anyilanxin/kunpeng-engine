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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import java.util.Optional;

/**
 * 引擎侧 job 投递端口：有活跃消费通道则直接投递，否则宣告该类型可消费（唤醒长轮询）。
 *
 * <p>由 broker 层实现（包装流协调器）；投递为异步即发即忘——送达失败由 broker 侧写 WITHDRAW 回退命令，引擎不感知。
 * 拉取/完成不经本端口，走标准命令通道（commandapi）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface JobDeliveryPort {

  /** 无可用消费通道时调用：宣告该 jobType 有新 job，唤醒挂起的长轮询（fire-and-forget） */
  void announceAvailable(String jobType);

  /** 挑选该 jobType 当前可用的消费通道；空 = 无通道，调用方走可消费宣告 */
  Optional<DeliveryChannel> pickStream(String jobType);

  /** 一条可投递的客户端通道 */
  interface DeliveryChannel {

    /** 选定目标的 worker（引擎据此落 lockOwner——记住谁消费的；重试/审计的归属依据） */
    String worker();

    /**
     * 投递单条已激活 job（非阻塞发送，实现内部合流出站）。
     *
     * @param jobKey job 日志键
     * @param partitionId job 所属分区（失败回退寻址）
     * @param deadline 激活截止毫秒时间戳
     * @param record 完整 job 记录（随行外发与失败回退）
     */
    void push(long jobKey, int partitionId, long deadline, JobRecord record);
  }
}
