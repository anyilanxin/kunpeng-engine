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

import com.anyilanxin.kunpeng.repository.admin.modules.admin.ImmutableAdminRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.delayed.ImmutableDelayedRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.key.ImmutableKeyRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.position.ImmutablePositionRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.source.ImmutableSourceRepository;

/**
 * 管理面只读仓储接口：source 等模块的查询能力聚合。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface AdminImmutableRepository {

  ImmutableAdminRepository adminRepository();

  ImmutableBusinessRepository businessRepository();

  ImmutableDelayedRepository delayedRepository();

  ImmutableKeyRepository keyRepository();

  ImmutablePositionRepository positionRepository();

  ImmutableSourceRepository sourceRepository();
}
