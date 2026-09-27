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
package com.anyilanxin.kunpeng.sink.rdbms.mapper;

import com.anyilanxin.kunpeng.sink.rdbms.model.SignalSubscriptionDbModel;
import org.apache.ibatis.annotations.Param;

/** 信号订阅表语句（XML：mapper/SignalSubscriptionMapper.xml）。 */
public interface SignalSubscriptionMapper {

  int insert(SignalSubscriptionDbModel model);

  int update(SignalSubscriptionDbModel model);

  int cleanupHistory(@Param("processInstanceId") long processInstanceId);

  int removePartition(@Param("resourceId") int resourceId);

  int clearAll();
}
