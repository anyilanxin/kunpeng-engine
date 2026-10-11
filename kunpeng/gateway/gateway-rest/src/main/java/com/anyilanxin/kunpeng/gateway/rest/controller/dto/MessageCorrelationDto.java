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
package com.anyilanxin.kunpeng.gateway.rest.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 消息投递请求：按消息名（及可选 correlationKey/processInstanceId）关联订阅中的流程。
 *
 * @param messageName 消息名
 * @param correlationKey 关联键
 * @param tenantId 租户
 * @param processInstanceId 定向投递的流程实例（&gt;0 时不再随机分区）
 * @param variable 携带变量（JSON 字符串）
 * @author zxuanhong
 * @since 2026.9.0
 */
public record MessageCorrelationDto(
    @NotBlank(message = "消息名不能为空") String messageName,
    String correlationKey,
    String tenantId,
    long processInstanceId,
    String variable) {}
