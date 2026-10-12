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
 * 表达式求值请求。
 *
 * @param expression 表达式
 * @param variables 求值上下文变量（JSON 字符串）
 * @param tenantId 租户
 * @param scopeKey 求值作用域（流程实例/活动实例 key，可空）
 * @author zxuanhong
 * @since 2026.9.0
 */
public record EvaluateExpressionDto(
    @NotBlank(message = "表达式不能为空") @Size(max = 2048, message = "表达式过长") String expression,
    String variables,
    @Size(max = 64, message = "租户标识过长") String tenantId,
    Long scopeKey) {}
