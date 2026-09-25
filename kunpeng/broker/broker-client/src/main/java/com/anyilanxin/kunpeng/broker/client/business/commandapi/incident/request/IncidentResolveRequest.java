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
package com.anyilanxin.kunpeng.broker.client.business.commandapi.incident.request;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.incident.IncidentAbstractRequest;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.incident.resolve.IncidentResolveRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.incident.CommandApiIncidentValueLifeCycle;
import org.apache.commons.lang3.StringUtils;

/**
 * 事件解决请求。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class IncidentResolveRequest extends IncidentAbstractRequest<IncidentResolveRequestRecord> {

  public IncidentResolveRequest(final long incidentId) {
    super(CommandApiIncidentValueLifeCycle.RESOLVE_REQUEST, new IncidentResolveRequestRecord());
    getValue().setIncidentId(incidentId);
    setKey(incidentId);
  }

  public IncidentResolveRequest setTenantId(final String tenantId) {
    if (StringUtils.isNotBlank(tenantId)) {
      getValue().setTenantId(tenantId);
    }
    return this;
  }
}
