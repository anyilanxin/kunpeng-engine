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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.addsign;

import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.RequestRecordValue;
import com.anyilanxin.kunpeng.structpack.JsonSerializable;
import java.util.List;

/**
 * 前加签请求：taskId 经记录 key 承载；assignees 为加签人名单。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface AddSignBeforeRequestRecordValue extends RequestRecordValue, JsonSerializable {

  List<String> getAssignees();

  AddSignBeforeRequestRecordValue setAssignees(final List<String> assignees);

  AddSignMode getAddSignMode();

  AddSignBeforeRequestRecordValue setAddSignMode(final AddSignMode addSignMode);
}
