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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.protocol.common;

/** 表示归属于某个租户（Tenant）的实体。 */
public interface TenantOwned {

  /** 默认租户标识。未启用多租户时，实体归属该默认租户。这样做是为了兼容将来启用多租户的场景。 */
  String DEFAULT_TENANT_IDENTIFIER = "<default>";

  /** 返回拥有该实体的租户标识。 */
  String getTenantId();
}
