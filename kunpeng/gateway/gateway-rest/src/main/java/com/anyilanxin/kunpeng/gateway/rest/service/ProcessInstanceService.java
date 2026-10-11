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
package com.anyilanxin.kunpeng.gateway.rest.service;

import com.anyilanxin.kunpeng.gateway.rest.controller.dto.ProcessInstanceCreateDto;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.ProcessInstanceVo;
import reactor.core.publisher.Mono;

/**
 * 流程实例业务。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ProcessInstanceService {

  /** 创建并启动流程实例。 */
  Mono<ProcessInstanceVo> create(ProcessInstanceCreateDto dto);

  /** 创建流程实例并等待执行结果（引擎侧尚无支撑，当前恒抛未实现）。 */
  Mono<ProcessInstanceVo> createWithResult(ProcessInstanceCreateDto dto);

  /** 按流程实例 key 取消实例。 */
  Mono<Void> cancel(long processInstanceKey);
}
