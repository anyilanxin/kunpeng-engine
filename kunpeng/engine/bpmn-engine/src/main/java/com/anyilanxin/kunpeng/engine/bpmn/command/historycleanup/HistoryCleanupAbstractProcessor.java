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
package com.anyilanxin.kunpeng.engine.bpmn.command.historycleanup;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessorSingleState;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.CatchEventBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.DelayBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.historycleanup.HistoryCleanupRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.timer.ImmutableTimerEventRepository;

/**
 * 历史清理命令处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class HistoryCleanupAbstractProcessor
    implements LogEventProcessorSingleState<HistoryCleanupRecord> {
  protected final LogEventWriter writer;
  protected final CatchEventBehavior catchEventBehavior;
  protected final ImmutableActivityInstanceRepository activityInstance;
  protected final ImmutableTimerEventRepository timer;
  protected final DelayBehavior delayBehavior;
  protected final ImmutableProcessInstanceRepository processInstance;
  protected final VariableBehavior variableBehavior;

  public HistoryCleanupAbstractProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    catchEventBehavior = behavior.catchEvent();
    final ImmutableBusinessRepository repository = writer.getRepository();
    activityInstance = repository.instanceRepository();
    timer = repository.timerEventRepository();
    delayBehavior = behavior.delayBehavior();
    processInstance = repository.processInstanceRepository();
    variableBehavior = behavior.variableBehavior();
  }

  @Override
  public abstract HistoryCleanupLifeCycle valueLifeCycle();

  @Override
  public ValueType valueType() {
    return ValueType.HISTORY_CLEANUP;
  }
}
