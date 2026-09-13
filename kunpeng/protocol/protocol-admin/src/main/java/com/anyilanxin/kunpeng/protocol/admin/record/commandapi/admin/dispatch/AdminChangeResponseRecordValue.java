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
package com.anyilanxin.kunpeng.protocol.admin.record.commandapi.admin.dispatch;

import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminClusterMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanRecordValue;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import com.anyilanxin.kunpeng.protocol.common.api.ResponseRecordValue;

/**
 * @author zxuanhong
 * @since
 */
public interface AdminChangeResponseRecordValue extends RecordValue, ResponseRecordValue {

  AdminDispatchPlanRecordValue getDispatchPlan();

  AdminClusterMetaRecordValue getClusterMeta();
}
