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
package com.anyilanxin.kunpeng.sink.runtime;

import static com.anyilanxin.kunpeng.repository.business.modules.sink.ImmutableSinkRepository.VALUE_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.config.messaging.PartitionMessagingService;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.eventlog.storage.EventStore;
import com.anyilanxin.kunpeng.eventlog.storage.EventStoreReader;
import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.ResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.sink.MutableSinkRepository;
import com.anyilanxin.kunpeng.repository.business.modules.sink.record.SinkStateEntry;
import com.anyilanxin.kunpeng.sink.api.RecordSink;
import com.anyilanxin.kunpeng.sink.registry.SinkDescriptor;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SinkService 恢复启动：持久化位置越过日志尾（跨数据世代的陈旧位置值）时不得让服务启动失败。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class SinkServiceTest {

  private static final String SINK_ID = "recording";

  private ActorScheduler scheduler;
  private FakeEventStore store;
  private EventLog eventLog;
  private InMemorySinkRepository sinkState;
  private RecordingCommunication communication;

  @BeforeEach
  void setUp() {
    scheduler =
        ActorScheduler.newActorScheduler().setSchedulerName("sink-service-test").build();
    scheduler.start();
    store = new FakeEventStore();
    eventLog =
        EventLog.builder()
            .withEventStore(store)
            .withLogName("sink-service-test")
            .withPartitionId(1)
            .build();
    sinkState = new InMemorySinkRepository();
    communication = new RecordingCommunication();
    RecordingSink.deliveredPositions.clear();
  }

  @AfterEach
  void tearDown() {
    scheduler.close();
    eventLog.close();
  }

  @Test
  @DisplayName("持久化位置越过空日志时钳到日志尾，服务正常启动")
  void shouldBootWhenPersistedPositionIsBeyondEmptyLog() throws Exception {
    sinkState.setSinkPosition(SINK_ID, 5);

    final var service = bootService();

    awaitTrue(() -> !communication.multicasts.isEmpty(), service);
    assertThat(service.getPhase().join()).isEqualTo(SinkPhase.RUNNING);
    assertThat(RecordingSink.deliveredPositions).isEmpty();
  }

  @Test
  @DisplayName("持久化位置越过日志尾时钳到末条，不回放旧记录且服务存活")
  void shouldBootAndParkAtEndWhenPersistedPositionIsBeyondLogTail() throws Exception {
    eventLog
        .newWriter()
        .tryAppend(
            WriteContext.INTERNAL, List.of(dummyEntry(1), dummyEntry(2), dummyEntry(3)), -1);
    assertThat(eventLog.getLastCommittedPosition()).isEqualTo(3);
    sinkState.setSinkPosition(SINK_ID, 5);

    final var service = bootService();

    awaitTrue(() -> !communication.multicasts.isEmpty(), service);
    assertThat(service.getPhase().join()).isEqualTo(SinkPhase.RUNNING);
    assertThat(RecordingSink.deliveredPositions).isEmpty();
    assertThat(service.getLowestPosition().join()).isEqualTo(5L);
  }

  private SinkService bootService() {
    final var messaging =
        new PartitionMessagingService(
            communication,
            new SingleMemberTopology(),
            PartitionId.from("test", 1),
            MemberId.from("self"));
    final var context =
        new SinkServiceContext()
            .actorName("sink-service-test-0")
            .eventLog(eventLog)
            .sinks(Map.of(new SinkDescriptor(SINK_ID, RecordingSink.class), new SinkInitInfo(0, null)))
            .repository(fakeBusinessRepository())
            .messaging(messaging)
            .broadcastInterval(Duration.ofMillis(100))
            .meterRegistry(new SimpleMeterRegistry());
    final var service = new SinkService(context, SinkPhase.RUNNING);
    service.startAsync(scheduler);
    return service;
  }

  private BusinessRepository fakeBusinessRepository() {
    return (BusinessRepository)
        Proxy.newProxyInstance(
            BusinessRepository.class.getClassLoader(),
            new Class<?>[] {BusinessRepository.class},
            (proxy, method, args) -> {
              if ("sinkRepository".equals(method.getName())) {
                return sinkState;
              }
              throw new UnsupportedOperationException(method.getName());
            });
  }

  private static AppendEntry dummyEntry(final long key) {
    return AppendEntry.of(key, bytes("m" + key), bytes("v" + key));
  }

  private static BufferWriter bytes(final String text) {
    final byte[] array = text.getBytes(StandardCharsets.UTF_8);
    return new DirectBufferWriter().wrap(new UnsafeBuffer(array), 0, array.length);
  }

  private static void awaitTrue(final BooleanSupplier condition, final SinkService service)
      throws InterruptedException {
    final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError(
            "Condition was not met within 10 seconds; health=%s".formatted(
                service.getHealthReport()));
      }
      Thread.sleep(10);
    }
  }

  /** 记录收到的 record position；由 SinkDescriptor 反射实例化，故以静态字段回传。 */
  public static final class RecordingSink implements RecordSink {

    static final List<Long> deliveredPositions = new CopyOnWriteArrayList<>();

    @Override
    public void sink(final BusinessEventRecord<?> record) {
      deliveredPositions.add(record.getPosition());
    }
  }

  /** 同步提交的内存存储（镜像 eventlog 测试的 InMemoryEventStore 同步模式）。 */
  private static final class FakeEventStore implements EventStore {

    private final List<UnsafeBuffer> blocks = new CopyOnWriteArrayList<>();
    private final List<long[]> ranges = new CopyOnWriteArrayList<>();
    private final List<CommitListener> commitListeners = new CopyOnWriteArrayList<>();

    @Override
    public EventStoreReader newReader() {
      return new FakeReader();
    }

    @Override
    public long getLastCommittedPosition() {
      return ranges.isEmpty() ? 0 : ranges.get(ranges.size() - 1)[1];
    }

    @Override
    public void append(
        final long firstPosition,
        final long lastPosition,
        final BufferWriter block,
        final AppendListener listener) {
      final var copy = new ExpandableArrayBuffer(block.getLength());
      block.write(copy, 0);
      blocks.add(new UnsafeBuffer(copy, 0, block.getLength()));
      ranges.add(new long[] {firstPosition, lastPosition});
      listener.onWrite(firstPosition, lastPosition);
      listener.onCommit(firstPosition, lastPosition);
      commitListeners.forEach(CommitListener::onCommit);
    }

    @Override
    public void addCommitListener(final CommitListener listener) {
      commitListeners.add(listener);
    }

    @Override
    public void removeCommitListener(final CommitListener listener) {
      commitListeners.remove(listener);
    }

    private final class FakeReader implements EventStoreReader {

      private int index;

      @Override
      public boolean hasNext() {
        return index < blocks.size();
      }

      @Override
      public DirectBuffer next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return blocks.get(index++);
      }

      @Override
      public void seek(final long position) {
        // 最后一个 first <= position 的块；越界钳到首/末块
        if (blocks.isEmpty() || position <= ranges.get(0)[0]) {
          index = 0;
          return;
        }
        index = blocks.size() - 1;
        for (int i = 0; i < ranges.size(); i++) {
          if (ranges.get(i)[0] > position) {
            index = i - 1;
            return;
          }
        }
      }

      @Override
      public void close() {
        // 无资源
      }
    }
  }

  /** sink 状态仓储的内存实现。 */
  private static final class InMemorySinkRepository implements MutableSinkRepository {

    private final Map<String, SinkStateEntry> entries = new ConcurrentHashMap<>();

    @Override
    public void setSinkPosition(final String sinkId, final long position) {
      entries.computeIfAbsent(sinkId, id -> new SinkStateEntry()).setPosition(position);
    }

    @Override
    public void removeSinkState(final String sinkId) {
      entries.remove(sinkId);
    }

    @Override
    public void setSinkState(final String sinkId, final long position, final DirectBuffer metadata) {
      final var entry = entries.computeIfAbsent(sinkId, id -> new SinkStateEntry());
      entry.setPosition(position);
      if (metadata != null) {
        entry.setMetadata(metadata);
      }
    }

    @Override
    public void initializeSinkState(
        final String sinkId,
        final long position,
        final DirectBuffer metadata,
        final long metadataVersion) {
      final var entry = new SinkStateEntry();
      entry.setPosition(position).setMetadataVersion(metadataVersion);
      if (metadata != null) {
        entry.setMetadata(metadata);
      }
      entries.put(sinkId, entry);
    }

    @Override
    public long getSinkPosition(final String sinkId) {
      final var entry = entries.get(sinkId);
      return entry == null ? VALUE_NOT_FOUND : entry.getPosition();
    }

    @Override
    public DirectBuffer getSinkMetadata(final String sinkId) {
      final var entry = entries.get(sinkId);
      return entry == null ? null : entry.getMetadata();
    }

    @Override
    public long getLowestPosition() {
      return entries.values().stream()
          .mapToLong(SinkStateEntry::getPosition)
          .min()
          .orElse(VALUE_NOT_FOUND);
    }

    @Override
    public long getMetadataVersion(final String sinkId) {
      final var entry = entries.get(sinkId);
      return entry == null ? 0 : entry.getMetadataVersion();
    }

    @Override
    public void visitSinkState(final BiConsumer<String, SinkStateEntry> consumer) {
      entries.forEach(consumer);
    }

    @Override
    public boolean hasSinks() {
      return !entries.isEmpty();
    }

    @Override
    public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
      // 测试不涉及分片导出
    }
  }

  /** 记录 multicast 调用；其余消息服务能力测试用不到。 */
  private static final class RecordingCommunication implements ClusterCommunicationService {

    final List<String> multicasts = new CopyOnWriteArrayList<>();

    @Override
    public <M> void multicast(
        final String subject,
        final M message,
        final Function<M, byte[]> encoder,
        final Set<MemberId> memberIds,
        final boolean reliable) {
      multicasts.add(subject);
    }

    @Override
    public void unsubscribe(final String subject) {
      // no-op
    }

    @Override
    public <M> void broadcast(
        final String subject,
        final M message,
        final Function<M, byte[]> encoder,
        final boolean reliable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <M> void unicast(
        final String subject,
        final M message,
        final Function<M, byte[]> encoder,
        final MemberId memberId,
        final boolean reliable) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <M, R> CompletableFuture<R> send(
        final String subject,
        final M message,
        final Function<M, byte[]> encoder,
        final Function<byte[], R> decoder,
        final MemberId toMemberId,
        final Duration timeout) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <M, R> void replyTo(
        final String subject,
        final Function<byte[], M> decoder,
        final Function<M, CompletableFuture<R>> handler,
        final Function<R, byte[]> encoder) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <M> void consume(
        final String subject,
        final Function<byte[], M> decoder,
        final java.util.function.Consumer<M> handler,
        final java.util.concurrent.Executor executor) {
      // no-op：leader 模式不订阅广播
    }

    @Override
    public <M> void consume(
        final String subject,
        final Function<byte[], M> decoder,
        final java.util.function.BiConsumer<MemberId, M> handler,
        final java.util.concurrent.Executor executor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <M, R> void replyTo(
        final String subject,
        final Function<byte[], M> decoder,
        final java.util.function.BiFunction<MemberId, M, R> handler,
        final Function<R, byte[]> encoder,
        final java.util.concurrent.Executor executor) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <M, R> void replyToAsync(
        final String subject,
        final Function<byte[], M> decoder,
        final Function<M, CompletableFuture<R>> handler,
        final Function<R, byte[]> encoder,
        final java.util.concurrent.Executor executor) {
      throw new UnsupportedOperationException();
    }
  }

  /** 恒返回一个"其它成员"，让 broadcast 真正走到 multicast。 */
  private static final class SingleMemberTopology implements ClusterTopologyService {

    @Override
    public List<PartitionMemberInfo> getPartitionMemberInfo(final PartitionId partitionId) {
      final var info = new PartitionMemberInfo();
      info.setMemberId(MemberId.from("other"));
      info.setPartitionId(partitionId);
      return List.of(info);
    }

    @Override
    public MemberId getPartitionLeader(final PartitionId partitionId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<PartitionId, List<PartitionMemberInfo>> getRaftGroup(final String groupName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<MemberId, List<PartitionMemberInfo>> getMemberPartitions() {
      throw new UnsupportedOperationException();
    }

    @Override
    public com.anyilanxin.kunpeng.cluster.utils.net.Address getPartitionBusinessAddress(
        final PartitionId partitionId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public org.agrona.collections.IntHashSet getActivitySourceIds() {
      throw new UnsupportedOperationException();
    }

    @Override
    public org.agrona.collections.IntHashSet getActivityPartitionIds() {
      throw new UnsupportedOperationException();
    }

    @Override
    public PartitionId getPartitionBySourceId(final int sourceId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getPartitionSource(final PartitionId partitionId) {
      throw new UnsupportedOperationException();
    }
  }
}
