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
package com.anyilanxin.kunpeng.protocol.business.record.command.usertask;

/**
 * 加签阶段：前加签（加签人先审、原审批人后审）/ 后加签（原审批人先审、加签人后审）。
 *
 * <p>⚠️ wire 契约：EnumProperty 按 ordinal 编解码，枚举常量只能尾部追加。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum AddSignPhase {
  NONE,
  /** 前加签 */
  BEFORE,
  /** 后加签 */
  AFTER
}
