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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 流程实例创建请求：processDefinitionKey 与 processDefinitionId 二选一，version 缺省 -1 表示最新版本。
 *
 * @param processDefinitionKey 流程定义 key
 * @param processDefinitionId 流程定义 id（与 key 二选一）
 * @param version 流程定义版本，缺省 -1 最新
 * @param tenantId 租户
 * @param variable 启动变量（JSON 字符串）
 * @author zxuanhong
 * @since 2026.9.0
 */
public record ProcessInstanceCreateDto(
    String processDefinitionKey,
    Long processDefinitionId,
    @Min(value = -1, message = "版本必须为正整数或 -1") Integer version,
    @Size(max = 64, message = "租户标识过长") String tenantId,
    String variable) {}
