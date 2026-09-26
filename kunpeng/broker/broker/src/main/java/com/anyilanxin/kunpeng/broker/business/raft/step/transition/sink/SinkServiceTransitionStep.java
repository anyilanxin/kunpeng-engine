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
package com.anyilanxin.kunpeng.broker.business.raft.step.transition.sink;

import com.anyilanxin.kunpeng.broker.business.raft.step.transition.BusinessTransitionContent;
import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionStep;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.eventlog.SkipPositionsFilter;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.sink.config.SinksConfig;
import com.anyilanxin.kunpeng.sink.debug.DebugLogSink;
import com.anyilanxin.kunpeng.sink.registry.SinkDescriptor;
import com.anyilanxin.kunpeng.sink.registry.SinkLoadException;
import com.anyilanxin.kunpeng.sink.registry.SinkRegistry;
import com.anyilanxin.kunpeng.sink.runtime.*;
import com.anyilanxin.kunpeng.utils.jar.ExternalJarLoadException;
import java.time.InstantSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 分区角色切换时装配 SinkService 的 transition step：先关闭旧服务，目标角色为 leader 时按当前配置 构建并启动新的 {@link
 * SinkService}，随后恢复持久化的暂停状态，并处理启动期间配置发生变化的 Sink。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SinkServiceTransitionStep implements TransitionStep<BusinessTransitionContent> {

  private final BiFunction<SinkServiceContext, SinkPhase, SinkService> sinkServiceBuilder;

  public SinkServiceTransitionStep() {
    sinkServiceBuilder = SinkService::new;
  }

  @Override
  public ActorFuture<Void> onLeader(
      final BusinessTransitionContent context, final long currentTerm) {
    final ActorFuture<Void> future = context.getConcurrencyControl().createFuture();
    context
        .getConcurrencyControl()
        .run(
            () -> {
              final SinkService sinkService = context.getSinkService();
              if (sinkService == null) {
                openSink(context, future, true);
              } else {
                future.complete(null);
              }
            });
    return future;
  }

  @Override
  public ActorFuture<Void> onFollower(
      final BusinessTransitionContent context, final long currentTerm) {
    final ActorFuture<Void> future = context.getConcurrencyControl().createFuture();
    context
        .getConcurrencyControl()
        .run(
            () -> {
              final SinkService sinkService = context.getSinkService();
              if (sinkService == null) {
                openSink(context, future, false);
              } else {
                future.complete(null);
              }
            });
    return future;
  }

  @Override
  public ActorFuture<Void> onInactive(
      final BusinessTransitionContent context, final long currentTerm) {
    final ActorFuture<Void> future = context.getConcurrencyControl().createFuture();
    context
        .getConcurrencyControl()
        .run(
            () -> {
              final SinkService sinkService = context.getSinkService();
              if (sinkService != null) {
                sinkService.close();
                context.setSinkService(null);
              }
              future.complete(null);
            });
    return future;
  }

  @Override
  public String getName() {
    return "Sink Service";
  }

  private void openSink(
      final BusinessTransitionContent context,
      final ActorFuture<Void> result,
      final boolean isLeader) {
    try {
      final var sinkDescriptors = getEnabledSinkDescriptors(context);
      final BrokerCfg brokerCfg = context.getBrokerCfg();
      final var sinkFilter =
          SkipPositionsFilter.of(context.getBrokerCfg().getSinking().skipRecords());
      final SinkServiceContext sinkCtx =
          new SinkServiceContext()
              .actorName(Actor.buildActorName("Sink", context.getRaftPartitionId().id()))
              .clock(InstantSource.system())
              .eventLog(context.getEventLog())
              .repository(context.getRepositoryFactory().create())
              .sinks(sinkDescriptors)
              .messaging(context.getPartitionMessagingService())
              .role(isLeader ? SinkRole.LEADER : SinkRole.FOLLOWER)
              .positionsToSkip(sinkFilter)
              .meterRegistry(context.getMeterRegistry());
      final SinkService sinkService = sinkServiceBuilder.apply(sinkCtx, SinkPhase.RUNNING);
      final var startFuture = sinkService.startAsync(context.getSchedulingService());
      startFuture.onComplete(
          (nothing, error) -> {
            if (error == null) {
              context.setSinkService(sinkService);
              // 服务创建后配置可能已变化
              try {
                disableOrEnableSinksIfConfigChanged(sinkDescriptors, context);
                result.complete(null);
              } catch (final Exception e) {
                result.completeExceptionally(e);
              }
            } else {
              result.completeExceptionally(error);
            }
          });
    } catch (final Exception e) {
      result.completeExceptionally(e);
    }
  }

  private void disableOrEnableSinksIfConfigChanged(
      final Map<SinkDescriptor, SinkInitInfo> startedSinks, final BusinessTransitionContent context)
      throws SinkLoadException, ExternalJarLoadException {
    final var currentEnabledSinks = getEnabledSinkDescriptors(context);
    for (final var sink : startedSinks.keySet()) {
      if (!currentEnabledSinks.containsKey(sink)) {
        context.getSinkService().disableSink(sink.getId());
      }
    }
    for (final var sinkEntry : currentEnabledSinks.entrySet()) {
      final var sink = sinkEntry.getKey();
      if (!startedSinks.containsKey(sink)) {
        context.getSinkService().enableSinkWithRetry(sink.getId(), sinkEntry.getValue(), sink);
      }
    }
  }

  private static Map<SinkDescriptor, SinkInitInfo> getEnabledSinkDescriptors(
      final BusinessTransitionContent context) throws SinkLoadException, ExternalJarLoadException {
    final SinksConfig sinksConfig = context.getSinksConfig();
    final SinkRegistry sinkRegistry = sinksConfig.registry();
    if (sinksConfig.enableDebugSink()) {
      sinkRegistry.load(DebugLogSink.defaultSinkId(), DebugLogSink.defaultConfig());
    }
    final Collection<SinkDescriptor> sinkDescriptors = sinkRegistry.getSinks().values();
    final Map<SinkDescriptor, SinkInitInfo> result = new HashMap<>();
    for (final SinkDescriptor sinkDescriptor : sinkDescriptors) {
      result.put(sinkDescriptor, SinkInitInfo.fresh());
    }
    return result;
  }
}
