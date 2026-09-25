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
package com.anyilanxin.kunpeng.broker.client.jobstream;

/**
 * job 流推送的消息主题与成员属性名：job 链路中除 PUSH 主题与可用性广播外，拉取/完成等全部走标准 broker 命令通道（commandapi）。
 *
 * <p>订阅快照不走消息主题——写入网关本地成员属性（{@link #SNAPSHOT_PROPERTY}），搭 SWIM 元数据通道（版本反熵 + owner 直拉）扩散到各
 * broker；PUSH 为请求-应答推送——应答即送达确认，失败在发送端就地可知并本地写 WITHDRAW 回退命令，无需反向失败回报主题。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JobStreamSubjects {
  /** 网关成员属性名：订阅快照（SBE 帧 base64），SWIM checkMetadata 检出变更后 bump metadataVersion 随 gossip 扩散 */
  public static final String SNAPSHOT_PROPERTY = "job-stream-snapshot";

  /** B→GW：实时流单条推送（请求-应答 PushResult；失败由 broker 侧本地回退） */
  public static final String PUSH = "job-stream-push";

  /** B→GW（广播）：jobType 有新 job 可消费，唤醒挂起的长轮询（fire-and-forget，载荷仅类型名） */
  public static final String READY = "job-ready";

  private JobStreamSubjects() {}
}
