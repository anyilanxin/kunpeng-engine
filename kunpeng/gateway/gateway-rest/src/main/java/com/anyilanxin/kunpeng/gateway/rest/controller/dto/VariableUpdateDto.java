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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 变量更新请求：scope 三元组（流程实例/活动实例/任务）定位写入位置。
 *
 * @param processInstanceId 流程实例 key
 * @param activityInstanceId 活动实例 key（局部变量时必填）
 * @param taskId 用户任务 key（任务局部变量时必填）
 * @param variable 变量内容（JSON 字符串）
 * @author zxuanhong
 * @since 2026.9.0
 */
public record VariableUpdateDto(
    @NotNull(message = "流程实例 key 不能为空") @Positive(message = "流程实例 key 必须为正数") Long processInstanceId,
    long activityInstanceId,
    long taskId,
    @NotBlank(message = "变量内容不能为空") String variable) {}
