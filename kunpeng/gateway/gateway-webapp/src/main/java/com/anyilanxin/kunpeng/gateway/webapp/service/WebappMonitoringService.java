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
package com.anyilanxin.kunpeng.gateway.webapp.service;

import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * 集群大屏指标服务：进程内直读 MeterRegistry + 内存环形缓冲，零外部系统。
 *
 * <p><b>读时采样</b>：无后台采样线程，前端轮询 {@link #sample()} 时才采样（距上次不足采样间隔直接返回现有窗口），
 * 无人观看零成本，窗口只在页面被观察时滚动。速率类指标由相邻两次采样的原始值差分得出（按每条时间线自己的采样间隔精确计算）， 均值/命中率在采样时算好，前端只画点。
 *
 * <p><b>节点切换</b>（2026-11-22 v1.5）：本服务以 {@link #SNAPSHOT_SUBJECT} 注册集群消息 reply 处理器
 * （先例：gateway-load-query），任意成员可单播拉取本节点完整窗口；{@link #sampleOf(String)} 按节点 id 本地直读或远端拉取。webapp
 * 未启用的节点无处理器，远端拉取报错——事实呈现，不做兜底。
 *
 * <p>窗口为进程内存活数据：重启清空、不持久化。meter 缺失则该时间线暂缺，出现后自动进入窗口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Service
public class WebappMonitoringService {

  /** 观察窗点数：5s 采样 × 360 点 ≈ 30 分钟 */
  static final int WINDOW_SIZE = 360;

  /** 最小采样间隔（前端轮询节拍与多浏览器共享时间线的守卫） */
  static final long SAMPLE_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(5);

  /** 远端快照单播超时 */
  private static final Duration SNAPSHOT_TIMEOUT = Duration.ofSeconds(8);

  /** 跨节点快照 subject（先例：gateway-load-query，见 GatewayTopologyManagerImpl） */
  public static final String SNAPSHOT_SUBJECT = "webapp-monitoring-snapshot";

  private static final String M_UPTIME = "process.uptime";
  private static final String M_CPU = "process.cpu.usage";
  private static final String M_HEAP_USED = "jvm.memory.used";
  private static final String M_HEAP_MAX = "jvm.memory.max";
  private static final String M_SINK_PHASE = "kunpeng.sink.phase";
  private static final String M_SINK_DELIVERED = "kunpeng.sink.position.delivered";
  private static final String M_SINK_COMMITTED = "kunpeng.sink.position.committed";
  private static final String M_BP_REJECTING = "commandapi.backpressure.rejecting";
  private static final String M_QUEUE_DEPTH = "scheduler.queue.depth";
  private static final String M_CACHE_HIT = "kunpeng.rocksdb.block.cache.hit";
  private static final String M_CACHE_MISS = "kunpeng.rocksdb.block.cache.miss";

  /** 消息载荷 JSON 编解码：关未知字段失败以容忍新旧版本混布（同 SBE decode-unknown 策略） */
  private static final ObjectMapper JSON =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final MeterRegistry meterRegistry;
  private final AtomixCluster atomixCluster;

  /** 时间线缓冲（按声明顺序稳定输出） */
  private final Map<String, Buffer> buffers = new LinkedHashMap<>();

  /** RATE 类的上一采样原始值（按时间线 key） */
  private final Map<String, Prev> prevRaw = new TreeMap<>();

  private long lastSampleNanos;
  private long lastSampledAtMillis;

  public WebappMonitoringService(
      final MeterRegistry meterRegistry, final AtomixCluster atomixCluster) {
    this.meterRegistry = meterRegistry;
    this.atomixCluster = atomixCluster;
  }

  /** 注册跨节点快照 reply 处理器；采样含 meter 同步读，直接在消息线程执行（与本地读时采样同语义） */
  @PostConstruct
  void registerSnapshotReply() {
    atomixCluster
        .getCommunicationService()
        .replyTo(
            SNAPSHOT_SUBJECT,
            Function.identity(),
            (_, _) -> sample(),
            WebappMonitoringService::encode,
            Runnable::run);
  }

  @PreDestroy
  void unregisterSnapshotReply() {
    atomixCluster.getCommunicationService().unsubscribe(SNAPSHOT_SUBJECT);
  }

  /** 单点：采样墙钟毫秒 + 值 */
  public record Point(long t, double v) {}

  /** stat 卡单值 */
  public record Stat(String key, double value, Map<String, String> tags) {}

  /** 一条时间线（tags 为该 meter 的全量标签） */
  public record Series(String key, Map<String, String> tags, List<Point> points) {}

  /** 全量快照：nodeId=应答节点（前端切换器的当前节点），summary 为最新值，series 为完整观察窗 */
  public record MonitoringVo(
      String nodeId, long sampledAt, int windowSize, List<Stat> summary, List<Series> series) {}

  private record Prev(double raw, long nanos) {}

  private record Buffer(String key, Map<String, String> tags, ArrayDeque<Point> points) {
    void append(final Point point) {
      points.addLast(point);
      while (points.size() > WINDOW_SIZE) {
        points.pollFirst();
      }
    }
  }

  /** 序列声明：meter 名即时间线 key 根，读法决定取值方式 */
  private record Spec(String meterName, Read read) {}

  private enum Read {
    /** gauge 原值 */
    GAUGE,
    /** 计数器差分速率（次/秒 或 字节/秒） */
    RATE,
    /** timer 平均耗时（毫秒） */
    TIMER_MEAN_MS,
    /** timer 最大耗时（毫秒） */
    TIMER_MAX_MS
  }

  /** 时序 catalog（C1–C13，sink 滞后与缓存命中率为派生序列见 appendDerived） */
  private static final List<Spec> SPECS =
      List.of(
          // C1 EL 三个水位
          new Spec("eventlog.position.written", Read.GAUGE),
          new Spec("eventlog.position.committed", Read.GAUGE),
          new Spec("eventlog.position.processed", Read.GAUGE),
          // C2 追加速率（按 context 的维度另成线）
          new Spec("eventlog.append.count", Read.RATE),
          new Spec("eventlog.append.context.count", Read.RATE),
          // C3 AIMD 流控
          new Spec("eventlog.flow.window", Read.GAUGE),
          new Spec("eventlog.flow.inflight", Read.GAUGE),
          // C4 背压
          new Spec("commandapi.backpressure.lag", Read.GAUGE),
          new Spec("commandapi.backpressure.rejected", Read.RATE),
          // C6/C7/C8 引擎与调度
          new Spec("kunpeng.engine.timers.queued", Read.GAUGE),
          new Spec("kunpeng.engine.timers.lag", Read.TIMER_MEAN_MS),
          new Spec("scheduler.timer.fired", Read.RATE),
          new Spec(M_QUEUE_DEPTH, Read.GAUGE),
          // C9/C10 journal
          new Spec("atomix_journal_append_rate", Read.RATE),
          new Spec("atomix_journal_append_data_rate", Read.RATE),
          new Spec("atomix_journal_size_bytes", Read.GAUGE),
          // C11 RocksDB 读写（ticker 累计值差分）
          new Spec("kunpeng.rocksdb.bytes.read", Read.RATE),
          new Spec("kunpeng.rocksdb.bytes.written", Read.RATE),
          // C13 选举
          new Spec("atomix_election_triggered_count", Read.RATE),
          new Spec("atomix_election_latency_in_ms", Read.GAUGE));

  /** 按节点取快照：空/本节点直读本地窗口；远端节点经集群消息单播拉取（远端缓冲由观看者轮询驱动滚动）。 */
  public MonitoringVo sampleOf(final String nodeId) {
    final String local = localNodeId();
    if (nodeId == null || nodeId.isBlank() || nodeId.equals(local)) {
      return sample();
    }
    final Member member = atomixCluster.getMembershipService().getMember(MemberId.from(nodeId));
    if (member == null) {
      throw new IllegalStateException("节点不在成员清单: " + nodeId);
    }
    if (!member.isReachable()) {
      throw new IllegalStateException("节点不可达: " + nodeId);
    }
    return atomixCluster
        .getCommunicationService()
        .send(
            SNAPSHOT_SUBJECT,
            new byte[0],
            Function.identity(),
            WebappMonitoringService::decode,
            member.id(),
            SNAPSHOT_TIMEOUT)
        .join();
  }

  private String localNodeId() {
    return atomixCluster.getMembershipService().getLocalMember().id().id();
  }

  /** 读时采样：距上次采样不足间隔直接返回现有窗口（summary 仍取实时值）；否则全量 catalog 采样后返回完整快照。 */
  public synchronized MonitoringVo sample() {
    final long nowNanos = System.nanoTime();
    if (lastSampleNanos != 0 && nowNanos - lastSampleNanos < SAMPLE_INTERVAL_NANOS) {
      return snapshot();
    }
    final long wall = System.currentTimeMillis();
    final List<Stat> summary = readSummary();
    for (final Spec spec : SPECS) {
      for (final Meter meter : meterRegistry.find(spec.meterName()).meters()) {
        appendSpec(spec, meter, wall);
      }
    }
    appendSinkLag(wall);
    appendCacheRatio(wall);
    lastSampleNanos = nowNanos;
    lastSampledAtMillis = wall;
    return snapshot();
  }

  private void appendSpec(final Spec spec, final Meter meter, final long wall) {
    final Map<String, String> tags = sortedTags(meter);
    final String key = seriesKey(spec.meterName(), tags);
    final double value =
        switch (spec.read()) {
          case GAUGE -> ((Gauge) meter).value();
          case RATE -> rateOf(key, cumulativeOf(meter));
          case TIMER_MEAN_MS -> timerMeanMs((Timer) meter);
          case TIMER_MAX_MS -> ((Timer) meter).max(TimeUnit.MILLISECONDS);
        };
    buffer(key, tags).append(new Point(wall, value));
  }

  /** 速率差分的原始累计值：Counter 取 count；RocksDB ticker 以 Gauge 形态暴露累计值，取 value */
  private static double cumulativeOf(final Meter meter) {
    return meter instanceof Counter counter ? counter.count() : ((Gauge) meter).value();
  }

  /** timer 均值：尚无观测时为 0 */
  private static double timerMeanMs(final Timer timer) {
    final long count = timer.count();
    return count == 0 ? 0 : timer.totalTime(TimeUnit.MILLISECONDS) / count;
  }

  /** 速率 = 本时间线相邻两次采样的原始值差分；窗口首点无基准记 0 */
  private double rateOf(final String key, final double raw) {
    final long now = System.nanoTime();
    final Prev prev = prevRaw.put(key, new Prev(raw, now));
    return prev == null ? 0 : (raw - prev.raw()) / ((now - prev.nanos()) / 1e9);
  }

  private Buffer buffer(final String key, final Map<String, String> tags) {
    return buffers.computeIfAbsent(key, k -> new Buffer(k, tags, new ArrayDeque<>()));
  }

  /** C5 sink 滞后：delivered − committed（按 sink 标签配对，缺任一侧则该 sink 无滞后线） */
  private void appendSinkLag(final long wall) {
    final Map<String, Double> delivered = gaugeByTag(M_SINK_DELIVERED, "sink");
    final Map<String, Double> committed = gaugeByTag(M_SINK_COMMITTED, "sink");
    for (final var entry : delivered.entrySet()) {
      final Double committedValue = committed.get(entry.getKey());
      if (committedValue == null) {
        continue;
      }
      final Map<String, String> tags = Map.of("sink", entry.getKey());
      buffer(seriesKey("sink.lag", tags), tags)
          .append(new Point(wall, entry.getValue() - committedValue));
    }
  }

  /** C12 块缓存命中率：hit 速率 / (hit 速率 + miss 速率)，按 partition 维度；无访问记 0 */
  private void appendCacheRatio(final long wall) {
    for (final Meter hit : meterRegistry.find(M_CACHE_HIT).meters()) {
      final String partition = hit.getId().getTag("partition");
      if (partition == null) {
        continue;
      }
      final Meter miss = meterRegistry.find(M_CACHE_MISS).tag("partition", partition).meter();
      if (miss == null) {
        continue;
      }
      final Map<String, String> tags = sortedTags(hit);
      final String hitKey = seriesKey("rocksdb.cache.hit.rate", tags);
      final String missKey = seriesKey("rocksdb.cache.miss.rate", tags);
      final double hitRate = rateOf(hitKey, ((Gauge) hit).value());
      final double missRate = rateOf(missKey, ((Gauge) miss).value());
      final double total = hitRate + missRate;
      buffer(seriesKey("rocksdb.cache.ratio", tags), tags)
          .append(new Point(wall, total > 0 ? hitRate / total : 0));
    }
  }

  /** stat 卡：进程/堆 + sink 相位/背压/队列深度（分区与领导数由前端从 /cluster/brokers 投影计算） */
  private List<Stat> readSummary() {
    final List<Stat> stats = new ArrayList<>();
    firstStat(M_UPTIME, "process.uptime").ifPresent(stats::add);
    firstStat(M_CPU, "process.cpu.usage").ifPresent(stats::add);
    stats.add(heapStat(M_HEAP_USED, "heap.used"));
    stats.add(heapStat(M_HEAP_MAX, "heap.max"));
    firstStat(M_SINK_PHASE, "sink.phase").ifPresent(stats::add);
    firstStat(M_BP_REJECTING, "backpressure.rejecting").ifPresent(stats::add);
    firstStat(M_QUEUE_DEPTH, "scheduler.queue.depth").ifPresent(stats::add);
    return stats;
  }

  private Optional<Stat> firstStat(final String meterName, final String key) {
    final Meter meter = meterRegistry.find(meterName).meter();
    return meter instanceof Gauge gauge
        ? Optional.of(new Stat(key, gauge.value(), Map.of()))
        : Optional.empty();
  }

  /** 堆内存聚合：area=heap 各池求和；max 侧 -1 表示未定义容量不计入 */
  private Stat heapStat(final String meterName, final String key) {
    double sum = 0;
    for (final Meter meter : meterRegistry.find(meterName).tag("area", "heap").meters()) {
      final double value = ((Gauge) meter).value();
      sum += value > 0 ? value : 0;
    }
    return new Stat(key, sum, Map.of());
  }

  private Map<String, Double> gaugeByTag(final String meterName, final String tagKey) {
    final Map<String, Double> result = new TreeMap<>();
    for (final Meter meter : meterRegistry.find(meterName).meters()) {
      final String tag = meter.getId().getTag(tagKey);
      if (tag != null) {
        result.put(tag, ((Gauge) meter).value());
      }
    }
    return result;
  }

  private MonitoringVo snapshot() {
    final List<Series> series = new ArrayList<>(buffers.size());
    for (final Buffer buffer : buffers.values()) {
      series.add(new Series(buffer.key(), buffer.tags(), List.copyOf(buffer.points())));
    }
    return new MonitoringVo(localNodeId(), lastSampledAtMillis, WINDOW_SIZE, readSummary(), series);
  }

  private static byte[] encode(final MonitoringVo vo) {
    try {
      return JSON.writeValueAsBytes(vo);
    } catch (final java.io.IOException e) {
      throw new IllegalStateException("监控快照序列化失败", e);
    }
  }

  private static MonitoringVo decode(final byte[] payload) {
    try {
      return JSON.readValue(payload, MonitoringVo.class);
    } catch (final java.io.IOException e) {
      throw new IllegalStateException("监控快照反序列化失败", e);
    }
  }

  private static Map<String, String> sortedTags(final Meter meter) {
    final Map<String, String> tags = new TreeMap<>();
    for (final var tag : meter.getId().getTags()) {
      tags.put(tag.getKey(), tag.getValue());
    }
    return tags;
  }

  private static String seriesKey(final String prefix, final Map<String, String> tags) {
    if (tags.isEmpty()) {
      return prefix;
    }
    final StringBuilder builder = new StringBuilder(prefix).append('{');
    final var iterator = tags.entrySet().iterator();
    while (iterator.hasNext()) {
      final var entry = iterator.next();
      builder.append(entry.getKey()).append('=').append(entry.getValue());
      if (iterator.hasNext()) {
        builder.append(',');
      }
    }
    return builder.append('}').toString();
  }
}
