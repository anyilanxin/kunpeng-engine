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
package com.anyilanxin.kunpeng.cluster.raft.metadata;

/** 业务元数据修改结果：success 时 index 为提交条目号；失败时 error 携带原因。 */
public record BusinessMetaUpdateResponse(boolean success, long index, String error) {

  public static BusinessMetaUpdateResponse ok(final long index) {
    return new BusinessMetaUpdateResponse(true, index, null);
  }

  public static BusinessMetaUpdateResponse noLeader() {
    return new BusinessMetaUpdateResponse(false, -1, "NO_LEADER");
  }

  public static BusinessMetaUpdateResponse error(final String message) {
    return new BusinessMetaUpdateResponse(false, -1, message);
  }
}
