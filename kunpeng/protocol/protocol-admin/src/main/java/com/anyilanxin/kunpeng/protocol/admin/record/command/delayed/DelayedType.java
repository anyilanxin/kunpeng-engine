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
package com.anyilanxin.kunpeng.protocol.admin.record.command.delayed;

/**
 * 管理分区扩缩容执行类型枚举，作为管理 Raft 执行服务消息处理的操作标识。
 *
 * @author zxuanhong
 * @since
 */
public enum DelayedType {
  /** 管理计划 */
  ADMIN_PLAN,
  /** 管理执行 */
  ADMIN_EXECUTION,
  /** 业务计划 */
  BUSINESS_PLAN,

  /** 业务执行 */
  BUSINESS_EXECUTION
}
