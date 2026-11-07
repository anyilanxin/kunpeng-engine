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
package com.anyilanxin.kunpeng.protocol.business;

/**
 * 引擎保留变量名契约（引擎/存储/导出面单源）：内部机械载荷（MI 集合快照、加签展开载荷）以此前缀命名—— 客户端路径写入/删除一律拒绝，导出面按前缀过滤不入业务变量表； MI
 * 计数器（nrOf* 等）为 BPMN 标准业务变量，不在保留之列。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface EngineReservedVariables {
  /** 引擎保留变量名前缀 */
  String PREFIX = "__kunpeng_";

  /** MI 集合快照（body 作用域，批创建窗口内子实例按游标取元素） */
  String MI_INPUT_COLLECTION = PREFIX + "mi_input_collection";

  /** 加签展开载荷（组活动实例作用域：assignees/mode/phase/taskKey/taskName） */
  String ADD_SIGN_PAYLOAD = PREFIX + "addsign_payload";

  /** 加签组计数器（组活动实例作用域：total/created/completed/terminated/cursor） */
  String ADD_SIGN_COUNTERS = PREFIX + "addsign_counters";
}
