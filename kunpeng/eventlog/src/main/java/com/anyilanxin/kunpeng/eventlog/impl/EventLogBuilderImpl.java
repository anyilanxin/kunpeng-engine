/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.eventlog.impl;

import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.eventlog.EventLogBuilder;
import com.anyilanxin.kunpeng.eventlog.FlowControlParams;
import com.anyilanxin.kunpeng.eventlog.storage.EventStore;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.Objects;

/** {@link com.anyilanxin.kunpeng.eventlog.EventLogBuilder} 默认实现 */
public final class EventLogBuilderImpl implements EventLogBuilder {

  private EventStore store;
  private String logName = "eventlog";
  private int partitionId = -1;
  private int maxBatchSize = 1024 * 1024;
  private Clock clock = Clock.systemUTC();
  private FlowControlParams params = FlowControlParams.defaults();
  private MeterRegistry registry;
  private int maxConcurrentAppends = 64;

  @Override
  public EventLogBuilder withEventStore(final EventStore store) {
    this.store = store;
    return this;
  }

  @Override
  public EventLogBuilder withLogName(final String logName) {
    this.logName = logName;
    return this;
  }

  @Override
  public EventLogBuilder withPartitionId(final int partitionId) {
    this.partitionId = partitionId;
    return this;
  }

  @Override
  public EventLogBuilder withMaxBatchSize(final int maxBatchSize) {
    this.maxBatchSize = maxBatchSize;
    return this;
  }

  @Override
  public EventLogBuilder withClock(final Clock clock) {
    this.clock = clock;
    return this;
  }

  @Override
  public EventLogBuilder withFlowControl(final FlowControlParams params) {
    this.params = params == null ? FlowControlParams.defaults() : params;
    return this;
  }

  @Override
  public EventLogBuilder withMeterRegistry(final MeterRegistry registry) {
    this.registry = registry;
    return this;
  }

  @Override
  public EventLogBuilder withMaxConcurrentAppends(final int slots) {
    this.maxConcurrentAppends = slots;
    return this;
  }

  @Override
  public EventLog build() {
    Objects.requireNonNull(store, "eventStore 必填");
    Objects.requireNonNull(logName, "logName 必填");
    if (partitionId < 0) {
      throw new IllegalArgumentException("partitionId 必填: " + partitionId);
    }
    return new EventLogImpl(
        store, logName, partitionId, maxBatchSize, clock, params, registry, maxConcurrentAppends);
  }
}
