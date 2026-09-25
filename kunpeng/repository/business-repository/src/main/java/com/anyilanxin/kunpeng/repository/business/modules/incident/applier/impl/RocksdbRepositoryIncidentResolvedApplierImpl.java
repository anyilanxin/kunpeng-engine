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
package com.anyilanxin.kunpeng.repository.business.modules.incident.applier.impl;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.incodent.IncidentRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.incident.MutableIncidentRepository;
import com.anyilanxin.kunpeng.repository.business.modules.incident.applier.RepositoryIncidentApplier;

/**
 * 事件解决事件应用器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbRepositoryIncidentResolvedApplierImpl
    implements RepositoryIncidentApplier<IncidentRecord> {
  private final MutableIncidentRepository incident;

  public RocksdbRepositoryIncidentResolvedApplierImpl(final BusinessRepository repository) {
    incident = repository.incidentRepository();
  }

  @Override
  public IncidentLifeCycle valueState() {
    return IncidentLifeCycle.RESOLVED;
  }

  @Override
  public void applyState(final long key, final IncidentRecord recordValue) {
    incident.delete(key);
  }
}
