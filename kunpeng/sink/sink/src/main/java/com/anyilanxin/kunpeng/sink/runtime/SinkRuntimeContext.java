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
package com.anyilanxin.kunpeng.sink.runtime;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.sink.api.context.RecordMatcher;
import com.anyilanxin.kunpeng.sink.api.context.SinkConfiguration;
import com.anyilanxin.kunpeng.sink.api.context.SinkContext;
import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import com.anyilanxin.kunpeng.utils.micrometer.PartitionKeyNames;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import java.time.InstantSource;
import org.slf4j.Logger;

/**
 * 交给单个 Sink 实例的 {@link SinkContext}；持有其作用域化的指标。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SinkRuntimeContext implements SinkContext, AutoCloseable {

  private static final RecordMatcher MATCH_ALL =
      new RecordMatcher() {
        @Override
        public boolean acceptsRecordType(final RecordType recordType) {
          return true;
        }

        @Override
        public boolean acceptsValueType(final ValueType valueType) {
          return true;
        }
      };

  private final Logger logger;
  private final SinkConfiguration configuration;
  private final int partitionId;
  private final InstantSource clock;
  private final CompositeMeterRegistry meterRegistry;
  private volatile RecordMatcher matcher = MATCH_ALL;

  public SinkRuntimeContext(
      final Logger logger,
      final SinkConfiguration configuration,
      final int partitionId,
      final MeterRegistry parentRegistry,
      final InstantSource clock) {
    this.logger = logger;
    this.configuration = configuration;
    this.partitionId = partitionId;
    this.clock = clock;
    meterRegistry =
        Micrometers.wrap(
            parentRegistry,
            Tags.concat(
                PartitionKeyNames.tags(partitionId), Tags.of("sinkId", configuration.getId())));
  }

  @Override
  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public InstantSource getClock() {
    return clock;
  }

  @Override
  public SinkConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public int getPartitionId() {
    return partitionId;
  }

  RecordMatcher getMatcher() {
    return matcher;
  }

  @Override
  public void setRecordMatcher(final RecordMatcher matcher) {
    this.matcher = matcher == null ? MATCH_ALL : matcher;
  }

  @Override
  public void close() {
    Micrometers.close(meterRegistry);
  }
}
