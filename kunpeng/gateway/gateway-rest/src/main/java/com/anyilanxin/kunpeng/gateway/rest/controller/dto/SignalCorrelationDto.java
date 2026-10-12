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
import jakarta.validation.constraints.Size;

/**
 * 信号广播请求。
 *
 * @param signalName 信号名
 * @param tenantId 租户
 * @param processInstanceId 定向广播的流程实例
 * @param variable 携带变量（JSON 字符串）
 * @author zxuanhong
 * @since 2026.9.0
 */
public record SignalCorrelationDto(
    @NotBlank(message = "信号名不能为空") @Size(max = 256, message = "信号名过长") String signalName,
    @Size(max = 64, message = "租户标识过长") String tenantId,
    long processInstanceId,
    String variable) {}
