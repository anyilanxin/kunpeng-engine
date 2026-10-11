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

import com.anyilanxin.kunpeng.gateway.rest.controller.dto.VariableRemoveDto;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.VariableUpdateDto;
import reactor.core.publisher.Mono;

/**
 * 变量业务。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface VariableService {

  /** 更新（合并写入）指定 scope 的变量。 */
  Mono<Void> update(VariableUpdateDto dto);

  /** 按变量名清单删除指定 scope 的变量。 */
  Mono<Void> remove(VariableRemoveDto dto);
}
