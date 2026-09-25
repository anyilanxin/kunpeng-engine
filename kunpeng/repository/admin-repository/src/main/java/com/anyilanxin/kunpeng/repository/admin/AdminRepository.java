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
package com.anyilanxin.kunpeng.repository.admin;

import com.anyilanxin.kunpeng.kvstore.TransactionContext;

/**
 * 管理面仓储顶层接口：读写能力与事务上下文的统一入口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface AdminRepository extends AdminMutableRepository, AdminImmutableRepository {
  TransactionContext getContext();

  AdminRepositoryAppliers getAppliers();
}
