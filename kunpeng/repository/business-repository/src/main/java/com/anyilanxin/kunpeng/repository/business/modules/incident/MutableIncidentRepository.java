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
package com.anyilanxin.kunpeng.repository.business.modules.incident;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.incodent.IncidentRecord;

/**
 * 事件域可写仓储接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface MutableIncidentRepository extends ImmutableIncidentRepository {
  void delete(long key);

  void save(long key, IncidentRecord record);

  void update(long key, IncidentRecord record);
}
