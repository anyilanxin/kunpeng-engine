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

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * 变量删除请求：按变量名清单在指定 scope 删除。
 *
 * @param processInstanceId 流程实例 key
 * @param activityInstanceId 活动实例 key
 * @param taskId 用户任务 key
 * @param removeVariable 待删除变量名清单
 * @author zxuanhong
 * @since 2026.9.0
 */
public record VariableRemoveDto(
    @NotNull(message = "流程实例 key 不能为空") @Positive(message = "流程实例 key 必须为正数") Long processInstanceId,
    long activityInstanceId,
    long taskId,
    @NotEmpty(message = "待删除变量名清单不能为空") List<String> removeVariable) {}
