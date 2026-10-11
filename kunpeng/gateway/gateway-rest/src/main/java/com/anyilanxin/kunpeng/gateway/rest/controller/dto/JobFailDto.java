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
import jakarta.validation.constraints.NotNull;

/**
 * job 失败请求：retries 为失败后剩余重试次数，retryBackOff 为重试退避（ms）。
 *
 * @param retries 剩余重试次数
 * @param retryBackOff 重试退避毫秒
 * @param errorMessage 失败原因
 * @author zxuanhong
 * @since 2026.9.0
 */
public record JobFailDto(
    @NotNull(message = "剩余重试次数不能为空") @Min(value = 0, message = "剩余重试次数不能为负") Integer retries,
    @NotNull(message = "重试退避不能为空") @Min(value = 0, message = "重试退避不能为负") Integer retryBackOff,
    String errorMessage) {}
