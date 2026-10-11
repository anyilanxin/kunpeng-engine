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
package com.anyilanxin.kunpeng.gateway.rest.service.vo;

import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass.JobDelivery;

/**
 * 推送/拉取到 worker 的单个 job 载荷；变量类字段原样透传 JSON 字符串。
 *
 * @param jobKey job 主键
 * @param type 消费类型
 * @param tenantId 租户
 * @param processInstanceKey 所属流程实例 key
 * @param elementId 关联元素 ID
 * @param worker 激活该 job 的 worker
 * @param retries 剩余重试次数
 * @param deadline 截止时间（UNIX epoch ms）
 * @param variables 任务变量（JSON 字符串）
 * @param customHeaders 自定义头（JSON 字符串）
 * @param activityInstanceKey 关联活动实例 key
 * @param jobKind job 种类序数值
 * @param listenerEventType 监听事件类型序数值（非监听 job 为 -1）
 * @param userTask 用户任务属性（JSON 字符串，非 user task 相关 job 为空）
 * @author zxuanhong
 * @since 2026.9.0
 */
public record JobVo(
    long jobKey,
    String type,
    String tenantId,
    long processInstanceKey,
    String elementId,
    String worker,
    int retries,
    long deadline,
    String variables,
    String customHeaders,
    long activityInstanceKey,
    int jobKind,
    int listenerEventType,
    String userTask) {

  public static JobVo of(final JobDelivery delivery) {
    return new JobVo(
        delivery.getJobKey(),
        delivery.getType(),
        delivery.getTenantId(),
        delivery.getProcessInstanceId(),
        delivery.getElementId(),
        delivery.getWorker(),
        delivery.getRetries(),
        delivery.getDeadline(),
        delivery.getVariables().toStringUtf8(),
        delivery.getCustomHeaders().toStringUtf8(),
        delivery.getActivityInstanceId(),
        delivery.getJobKind(),
        delivery.getListenerEventType(),
        delivery.getUserTask().toStringUtf8());
  }
}
