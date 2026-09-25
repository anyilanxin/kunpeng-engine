/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.protocol.business.impl;

/**
 * 命令拒绝类型：记录元数据中以名称字符串存储（见 {@link RecordMetadata}）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum RejectionType {
  NULL_VAL,
  INVALID_ARGUMENT,
  NOT_FOUND,
  ALREADY_EXISTS,
  INVALID_STATE,
  PROCESSING_ERROR
}
