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
package com.anyilanxin.kunpeng.eventlog;

/**
 * 追加结果：成功携带批的 position 区间；失败携带原因。
 *
 * <p>成功语义为"已定序"——position 已唯一分配；多写者并发下批帧可能延后瞬间才提交到存储 （有序提交链保证按 firstPosition 升序落盘），消费方以 commit
 * 通知为准。
 */
public sealed interface AppendResult {

  record Appended(long firstPosition, long lastPosition) implements AppendResult {}

  record Rejected(RejectionReason reason) implements AppendResult {}

  enum RejectionReason {
    /** 日志已关闭 */
    CLOSED,
    /** 参数非法（空批/超限长度等） */
    INVALID_ARGUMENT,
    /** 在途窗口耗尽（仅 USER_COMMAND） */
    REQUEST_WINDOW_EXHAUSTED,
    /** 写入速率耗尽（仅 USER_COMMAND） */
    WRITE_RATE_EXHAUSTED
  }
}
