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
package com.anyilanxin.kunpeng.repository.business.modules.processinstance.applier.impl;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.MutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.applier.RepositoryProcessInstanceApplier;

/**
 * 流程实例已终止事件应用器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbRepositoryProcessInstanceTerminatedApplierImpl
    implements RepositoryProcessInstanceApplier<ProcessInstanceRecord> {
  private final MutableProcessInstanceRepository processInstance;

  public RocksdbRepositoryProcessInstanceTerminatedApplierImpl(
      final BusinessRepository repository) {
    processInstance = repository.processInstanceRepository();
  }

  @Override
  public void applyState(final long key, final ProcessInstanceRecord recordValue) {
    processInstance.delete(key);
  }

  @Override
  public ProcessInstanceLifeCycle valueState() {
    return ProcessInstanceLifeCycle.TERMINATED;
  }
}
