/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import { CloudUploadOutlined, DashboardOutlined, FieldTimeOutlined, PartitionOutlined } from '@ant-design/icons';
import { Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { api } from '../api';
import { usePolling } from '../usePolling';
import { useThemeMode } from '../theme/ThemeModeContext';
import { useBrokers } from '../data/ClusterDataContext';
import EChart from '../components/EChart';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionHeader from '../components/SectionHeader';

/** 后端 MonitoringSnapshotVo 镜像（cluster 模块跨进程 JSON 契约） */
interface Point {
  t: number;
  v: number;
}
interface Stat {
  key: string;
  value: number;
  tags: Record<string, string>;
}
interface Series {
  key: string;
  tags: Record<string, string>;
  points: Point[];
}
interface MonitoringVo {
  nodeId: string;
  sampledAt: number;
  windowSize: number;
  summary: Stat[];
  series: Series[];
}

/** Sink 相位枚举序（SinkService 生命周期） */
const PHASES = ['running', 'paused', 'softPaused', 'closed'];

/** 曲线调色板（明暗两模式通用饱和色） */
const PALETTE = ['#246bfd', '#35c194', '#b9a3ef', '#3fc5d4', '#f2a13c', '#7a63c3', '#d8404a', '#51637e'];

interface LineDef {
  match: string;
  nameKey: string;
}
interface ChartDef {
  titleKey: string;
  unit: 'count' | 'ops' | 'bytes' | 'bytesps' | 'ms' | 'pct';
  lines: LineDef[];
  /** 独占整行（多系列需要大版面的图） */
  wide?: boolean;
  /** 无数据时的说明（如数据源为默认关闭的配置项）；缺省用通用空态 */
  hintKey?: string;
}
interface SectionDef {
  titleKey: string;
  descKey: string;
  charts: ChartDef[];
}

const SECTIONS: SectionDef[] = [
  {
    titleKey: 'app.monitoring.secThroughput',
    descKey: 'app.monitoring.secThroughputDesc',
    charts: [
      {
        titleKey: 'app.monitoring.c1',
        unit: 'count',
        wide: true,
        lines: [
          { match: 'eventlog.position.written', nameKey: 'app.monitoring.written' },
          { match: 'eventlog.position.committed', nameKey: 'app.monitoring.committed' },
          { match: 'eventlog.position.processed', nameKey: 'app.monitoring.processed' },
        ],
      },
      {
        titleKey: 'app.monitoring.c2',
        unit: 'ops',
        lines: [
          { match: 'eventlog.append.count', nameKey: 'app.monitoring.appendTotal' },
          { match: 'eventlog.append.context.count', nameKey: 'app.monitoring.appendContext' },
        ],
      },
      {
        titleKey: 'app.monitoring.c3',
        unit: 'count',
        lines: [
          { match: 'eventlog.flow.window', nameKey: 'app.monitoring.flowWindow' },
          { match: 'eventlog.flow.inflight', nameKey: 'app.monitoring.flowInflight' },
        ],
      },
      {
        titleKey: 'app.monitoring.c4',
        unit: 'count',
        hintKey: 'app.monitoring.bpHint',
        lines: [
          { match: 'commandapi.backpressure.lag', nameKey: 'app.monitoring.bpLag' },
          { match: 'commandapi.backpressure.rejected', nameKey: 'app.monitoring.bpRejected' },
        ],
      },
      {
        titleKey: 'app.monitoring.c5',
        unit: 'count',
        lines: [{ match: 'sink.lag', nameKey: 'app.monitoring.sinkLag' }],
      },
    ],
  },
  {
    titleKey: 'app.monitoring.secEngine',
    descKey: 'app.monitoring.secEngineDesc',
    charts: [
      {
        titleKey: 'app.monitoring.c6',
        unit: 'count',
        lines: [
          { match: 'kunpeng.engine.timers.queued', nameKey: 'app.monitoring.timersQueued' },
          { match: 'kunpeng.engine.timers.lag', nameKey: 'app.monitoring.timersLag' },
        ],
      },
      {
        titleKey: 'app.monitoring.c7',
        unit: 'ops',
        lines: [{ match: 'scheduler.timer.fired', nameKey: 'app.monitoring.timersFired' }],
      },
      {
        titleKey: 'app.monitoring.c8',
        unit: 'count',
        wide: true,
        lines: [{ match: 'scheduler.queue.depth', nameKey: 'app.monitoring.depth' }],
      },
    ],
  },
  {
    titleKey: 'app.monitoring.secStorage',
    descKey: 'app.monitoring.secStorageDesc',
    charts: [
      {
        titleKey: 'app.monitoring.c9',
        unit: 'count',
        lines: [
          { match: 'atomix_journal_append_rate', nameKey: 'app.monitoring.journalEntries' },
          { match: 'atomix_journal_append_data_rate', nameKey: 'app.monitoring.journalKiB' },
        ],
      },
      {
        titleKey: 'app.monitoring.c10',
        unit: 'bytes',
        lines: [{ match: 'atomix_journal_size_bytes', nameKey: 'app.monitoring.journalSize' }],
      },
      {
        titleKey: 'app.monitoring.c11',
        unit: 'bytesps',
        hintKey: 'app.monitoring.rocksHint',
        lines: [
          { match: 'kunpeng.rocksdb.bytes.read', nameKey: 'app.monitoring.rocksRead' },
          { match: 'kunpeng.rocksdb.bytes.written', nameKey: 'app.monitoring.rocksWrite' },
        ],
      },
      {
        titleKey: 'app.monitoring.c12',
        unit: 'pct',
        hintKey: 'app.monitoring.rocksHint',
        lines: [{ match: 'rocksdb.cache.ratio', nameKey: 'app.monitoring.cacheRatio' }],
      },
      {
        titleKey: 'app.monitoring.c13',
        unit: 'count',
        wide: true,
        lines: [
          { match: 'atomix_election_triggered_count', nameKey: 'app.monitoring.electionRate' },
          { match: 'atomix_election_latency_in_ms', nameKey: 'app.monitoring.electionLatency' },
        ],
      },
    ],
  },
];

const fmtBytes = (v: number) => {
  if (!Number.isFinite(v) || v <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let value = v;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }
  return `${value >= 10 ? Math.round(value) : value.toFixed(1)} ${units[unit]}`;
};

const fmtNum = (v: number) => {
  if (!Number.isFinite(v)) return '-';
  const abs = Math.abs(v);
  if (abs >= 1e9) return `${(v / 1e9).toFixed(1)}B`;
  if (abs >= 1e6) return `${(v / 1e6).toFixed(1)}M`;
  if (abs >= 1e4) return `${(v / 1e3).toFixed(1)}k`;
  return abs >= 10 ? String(Math.round(v)) : v.toFixed(1);
};

const fmtDuration = (seconds: number) => {
  const d = Math.floor(seconds / 86400);
  const h = Math.floor((seconds % 86400) / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (d > 0) return `${d}d ${h}h`;
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
};

export default function MonitoringPage() {
  const { t } = useTranslation();
  const { mode } = useThemeMode();
  const brokers = useBrokers();
  /** 切换节点：broker memberId（监控数据来自 broker 节点，无本节点视角）；undefined = 初始化中 */
  const [node, setNode] = useState<string>();
  useEffect(() => {
    if (node || brokers.length === 0) return;
    setNode((brokers.find((broker) => broker.state !== 'OFFLINE') ?? brokers[0]).memberId);
  }, [brokers, node]);
  const { data, error, refresh } = usePolling<MonitoringVo>(
    () =>
      node
        ? api<MonitoringVo>(`/monitoring?node=${encodeURIComponent(node)}`)
        : new Promise<MonitoringVo>(() => {}),
    5000,
  );
  const pageRef = useRef<HTMLDivElement>(null);
  const [fillHeight, setFillHeight] = useState<number>();

  useEffect(() => {
    const measure = () => {
      const page = pageRef.current;
      if (!page) return;
      const content = document.querySelector('.app-content');
      const footer = document.querySelector('.app-footer') as HTMLElement | null;
      const contentPadding = content ? parseFloat(getComputedStyle(content).paddingBottom) : 0;
      const footerStyle = footer ? getComputedStyle(footer) : null;
      const footerSpace =
        footer && footerStyle ? footer.offsetHeight + parseFloat(footerStyle.marginTop) + parseFloat(footerStyle.marginBottom) : 0;
      setFillHeight(Math.max(420, window.innerHeight - page.getBoundingClientRect().top - contentPadding - footerSpace));
    };
    measure();
    window.addEventListener('resize', measure);
    return () => window.removeEventListener('resize', measure);
  }, []);

  /** 切换节点立即拉取，不等下个轮询周期 */
  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [node]);

  const summary = useMemo(() => Object.fromEntries((data?.summary ?? []).map((stat) => [stat.key, stat])), [data]);

  /** 观看节点的分区投影：注册表权威清单（/cluster/brokers）按 memberId 过滤，含业务与管理分区组 */
  const viewingPartitions = useMemo(() => {
    const nodeId = data?.nodeId;
    return nodeId ? (brokers.find((broker) => broker.memberId === nodeId)?.partitions ?? []) : [];
  }, [brokers, data?.nodeId]);
  const partitionTotal = viewingPartitions.length;
  const leaderCount = viewingPartitions.filter((partition) => partition.role === 'LEADER').length;
  const followerCount = viewingPartitions.filter((partition) => partition.role === 'FOLLOWER').length;

  const chartHasSeries = (chart: ChartDef) => {
    const all = data?.series ?? [];
    return chart.lines.some(
      (line) => all.some((s) => s.key === line.match || s.key.startsWith(`${line.match}{`)),
    );
  };

  const optionFor = useMemo(
    () =>
      (chart: ChartDef) => {
        const rootStyle = getComputedStyle(document.documentElement);
        const cssVar = (name: string) => rootStyle.getPropertyValue(name).trim();
        const all = data?.series ?? [];
        const lines = chart.lines.flatMap((line) => {
          const found = all.filter((s) => s.key === line.match || s.key.startsWith(`${line.match}{`));
          const suffix = found.length > 1;
          const shorten = (raw: string) => raw.replace(/^eventLog-/, '').replace(/-partition-partition-/, '-');
          return found.map((s) => ({
            name:
              t(line.nameKey) +
              (suffix
                ? ` · ${shorten(s.tags.partition ?? s.tags.sink ?? s.tags.log ?? s.tags.context ?? s.tags.partitionGroupName ?? Object.values(s.tags)[0] ?? '')}`
                : ''),
            points: s.points.map((p) => [p.t, p.v] as [number, number]),
          }));
        });
        const formatY = (v: number) => {
          switch (chart.unit) {
            case 'pct':
              return `${(v * 100).toFixed(0)}%`;
            case 'bytes':
              return fmtBytes(v);
            case 'bytesps':
              return `${fmtBytes(v)}/s`;
            case 'ops':
              return `${fmtNum(v)}/s`;
            case 'ms':
              return `${fmtNum(v)}ms`;
            default:
              return fmtNum(v);
          }
        };
        return {
          tooltip: { trigger: 'axis', confine: true, valueFormatter: (v: number) => formatY(v) },
          legend: { show: lines.length > 1, type: 'scroll', bottom: 0, icon: 'roundRect', itemWidth: 12, itemHeight: 8, textStyle: { color: cssVar('--muted'), fontSize: 10 } },
          grid: { left: 10, right: 18, top: 14, bottom: lines.length > 1 ? 26 : 8, containLabel: true },
          xAxis: {
            type: 'time',
            axisLine: { lineStyle: { color: cssVar('--accent-line') } },
            axisTick: { show: false },
            axisLabel: { color: cssVar('--muted'), hideOverlap: true },
            splitLine: { show: false },
          },
          yAxis: {
            type: 'value',
            axisLabel: { color: cssVar('--muted'), formatter: formatY },
            splitLine: { lineStyle: { color: cssVar('--line-soft') } },
          },
          series: lines.map((line, index) => ({
            name: line.name,
            type: 'line',
            showSymbol: false,
            data: line.points,
            lineStyle: { width: 1.6 },
            color: PALETTE[index % PALETTE.length],
            emphasis: { focus: 'series' },
          })),
        };
      },
    [data, t, mode],
  );

  const cpuStat = summary['process.cpu.usage'];
  const heapUsed = summary['heap.used']?.value ?? 0;
  const heapMax = summary['heap.max']?.value ?? 0;
  const heapPct = heapMax > 0 ? heapUsed / heapMax : undefined;
  const phaseStat = summary['sink.phase'];
  const bpStat = summary['backpressure.rejecting'];
  const depthStat = summary['scheduler.queue.depth'];

  const nodeOptions = useMemo<{ value: string; label: string; disabled?: boolean }[]>(
    () =>
      brokers.map((broker) => ({
        value: broker.memberId,
        label: broker.memberId,
        disabled: broker.state === 'OFFLINE',
      })),
    [brokers],
  );

  return (
    <div className="page page--fill" ref={pageRef} style={fillHeight ? { height: fillHeight } : undefined}>
      <PageHeader
        compact
        eyebrow="LIVE METRICS"
        title={t('app.menu.monitoring')}
        description={t('app.monitoring.heroDesc')}
        toolbar={
          <Select
            className="monitoring-node-select"
            size="small"
            value={node}
            onChange={(value) => setNode(value)}
            options={nodeOptions}
            aria-label={t('app.monitoring.nodeLabel')}
          />
        }
      />

      <section className="metrics-grid">
        <MetricCard
          title={t('app.monitoring.cardPartitions')}
          value={partitionTotal ? <>{leaderCount}<small> / {partitionTotal}</small></> : '—'}
          icon={<PartitionOutlined />}
          tone="blue"
          footer={partitionTotal ? t('app.monitoring.leaderFooter', { leaders: leaderCount, followers: followerCount }) : t('app.monitoring.waiting')}
        />
        <MetricCard
          title={t('app.monitoring.cardUptime')}
          value={summary['process.uptime'] ? fmtDuration(summary['process.uptime'].value) : '—'}
          icon={<FieldTimeOutlined />}
          tone="cyan"
          footer={cpuStat ? `${t('app.monitoring.cpu')} ${(cpuStat.value * 100).toFixed(1)}%` : t('app.monitoring.waiting')}
        />
        <MetricCard
          title={t('app.monitoring.cardHeap')}
          value={heapPct !== undefined ? <>{Math.round(heapPct * 100)}<small> %</small></> : '—'}
          icon={<DashboardOutlined />}
          tone="violet"
          footer={`${fmtBytes(heapUsed)} / ${fmtBytes(heapMax)}`}
        />
        <MetricCard
          title={t('app.monitoring.cardSink')}
          value={phaseStat ? t(`app.monitoring.phase.${PHASES[Math.round(phaseStat.value)] ?? 'closed'}`) : '—'}
          icon={<CloudUploadOutlined />}
          tone="green"
          footer={
            bpStat || depthStat
              ? `${bpStat ? (Math.round(bpStat.value) === 1 ? t('app.monitoring.bpRejecting') : t('app.monitoring.bpPassing')) : ''}${bpStat && depthStat ? ' · ' : ''}${depthStat ? `${t('app.monitoring.queueDepth')} ${Math.round(depthStat.value)}` : ''}`
              : t('app.monitoring.waiting')
          }
        />
      </section>

      {error && !data && <div className="monitoring-error">{error}</div>}

      <div className="monitoring-scroll">
        {SECTIONS.map((section) => (
          <section key={section.titleKey}>
            <SectionHeader title={t(section.titleKey)} description={t(section.descKey)} />
            <div className="monitoring-charts">
              {section.charts.map((chart) => (
                <div className={`panel monitoring-chart${chart.wide ? ' monitoring-chart--wide' : ''}`} key={chart.titleKey}>
                  <div className="monitoring-chart-head">
                    <SectionHeader title={t(chart.titleKey)} />
                  </div>
                  {chartHasSeries(chart) ? (
                    <EChart option={optionFor(chart)} height={chart.wide ? 300 : 244} />
                  ) : (
                    <div className="monitoring-chart-empty" style={{ height: chart.wide ? 300 : 244 }}>
                      {t(chart.hintKey ?? 'app.monitoring.noData')}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  );
}
