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

import com.anyilanxin.kunpeng.cluster.config.messaging.PartitionMessagingService;
import com.anyilanxin.kunpeng.eventlog.EntryFilter;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.sink.registry.SinkDescriptor;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.InstantSource;
import java.util.Map;
import java.util.Objects;

/**
 * 一个 {@link SinkService} 的接线配置：服务的分区、驱动的 Sink ，以及所需的协作者。 通过流式 setter 构建；除注明默认值者外，全部必填。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SinkServiceContext {

  public static final Duration DEFAULT_BROADCAST_INTERVAL = Duration.ofSeconds(15);

  private String actorName;
  private EventLog eventLog;
  private Map<SinkDescriptor, SinkInitInfo> sinks = Map.of();
  private BusinessRepository repository;
  private PartitionMessagingService messaging;
  private SinkRole role = SinkRole.LEADER;
  private Duration broadcastInterval = DEFAULT_BROADCAST_INTERVAL;
  private EntryFilter positionsToSkip;
  private MeterRegistry meterRegistry;
  private InstantSource clock = InstantSource.system();

  public String getActorName() {
    return actorName;
  }

  public EventLog getEventLog() {
    return eventLog;
  }

  public Map<SinkDescriptor, SinkInitInfo> getSinks() {
    return sinks;
  }

  public BusinessRepository getRepository() {
    return repository;
  }

  public PartitionMessagingService getMessaging() {
    return messaging;
  }

  public SinkRole getRole() {
    return role;
  }

  public Duration getBroadcastInterval() {
    return broadcastInterval;
  }

  public EntryFilter getPositionsToSkip() {
    return positionsToSkip;
  }

  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  public InstantSource getClock() {
    return clock;
  }

  public SinkServiceContext actorName(final String actorName) {
    this.actorName = actorName;
    return this;
  }

  public SinkServiceContext eventLog(final EventLog eventLog) {
    this.eventLog = eventLog;
    return this;
  }

  public SinkServiceContext sinks(final Map<SinkDescriptor, SinkInitInfo> sinks) {
    this.sinks = sinks;
    return this;
  }

  public SinkServiceContext repository(final BusinessRepository repository) {
    this.repository = repository;
    return this;
  }

  public SinkServiceContext messaging(final PartitionMessagingService messaging) {
    this.messaging = messaging;
    return this;
  }

  public SinkServiceContext role(final SinkRole role) {
    this.role = Objects.requireNonNull(role, "role must not be null");
    return this;
  }

  public SinkServiceContext broadcastInterval(final Duration broadcastInterval) {
    this.broadcastInterval =
        broadcastInterval == null ? DEFAULT_BROADCAST_INTERVAL : broadcastInterval;
    return this;
  }

  public SinkServiceContext positionsToSkip(final EntryFilter positionsToSkip) {
    this.positionsToSkip = positionsToSkip;
    return this;
  }

  public SinkServiceContext meterRegistry(final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    return this;
  }

  public SinkServiceContext clock(final InstantSource clock) {
    this.clock = clock == null ? InstantSource.system() : clock;
    return this;
  }
}
