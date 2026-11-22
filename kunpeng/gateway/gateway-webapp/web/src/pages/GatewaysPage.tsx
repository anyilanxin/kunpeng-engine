import { useMemo, useState } from 'react';
import { SearchOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { Alert, Button, Empty, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useTranslation } from 'react-i18next';
import { api } from '../api';
import { usePolling } from '../usePolling';
import { useThemeMode } from '../theme/ThemeModeContext';
import { ErrorAlert } from '../components/ErrorAlert';
import EChart from '../components/EChart';
import PageHeader from '../components/PageHeader';
import SectionHeader from '../components/SectionHeader';
import WorkerPulseVisual from '../components/WorkerPulseVisual';
import type { GatewayVo } from '../types';

interface LoadVo {
  nodeId: string;
  activeConnections: number;
}

export default function GatewaysPage() {
  const { t } = useTranslation();
  const { mode } = useThemeMode();
  const { data, error, loading } = usePolling<GatewayVo[]>(() => api<GatewayVo[]>('/gateways'));
  const [loads, setLoads] = useState<Record<string, LoadVo>>({});

  if (error && !data) {
    return <ErrorAlert message={error} />;
  }

  const gateways = data ?? [];

  const queryLoad = async (address: string) => {
    let value: LoadVo;
    try {
      value = await api<LoadVo>(`/gateways/load?grpcAddress=${encodeURIComponent(address)}`);
    } catch {
      value = { nodeId: '-', activeConnections: -1 };
    }
    setLoads((prev) => ({ ...prev, [address]: value }));
  };

  const queryAll = () => {
    gateways.forEach((g) => queryLoad(g.grpcAddress));
  };

  const hasKnownLoad = gateways.some((g) => loads[g.grpcAddress]?.activeConnections >= 0);

  // ECharts 渲染到 canvas，fillStyle 不解析 var()；取色时把 CSS 变量解析成具体色值（mode 变化触发重算）
  const loadOption = useMemo(() => {
    const rootStyle = getComputedStyle(document.documentElement);
    const cssVar = (name: string) => rootStyle.getPropertyValue(name).trim();
    const entries = gateways.map((g) => ({
      address: g.grpcAddress.replace(/^https?:\/\//, ''),
      value: loads[g.grpcAddress]?.activeConnections ?? 0,
      known: g.grpcAddress in loads,
    }));
    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 8, right: 16, top: 18, bottom: 0, containLabel: true },
      xAxis: {
        type: 'category',
        data: entries.map((e) => e.address),
        axisLine: { lineStyle: { color: cssVar('--accent-line') } },
        axisTick: { show: false },
        axisLabel: { color: cssVar('--muted') },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        splitLine: { lineStyle: { color: cssVar('--line-soft') } },
        axisLabel: { color: cssVar('--muted') },
      },
      series: [
        {
          name: t('app.gateways.chartSeries'),
          type: 'bar',
          barWidth: 26,
          itemStyle: { borderRadius: [6, 6, 0, 0], color: '#246bfd' },
          label: { show: true, position: 'top', color: cssVar('--text-2') },
          data: entries.map((e) => (e.known && e.value >= 0 ? e.value : null)),
        },
      ],
    };
  }, [gateways, loads, t, mode]);

  const columns: ColumnsType<GatewayVo> = [
    { title: t('app.gateways.colNode'), dataIndex: 'nodeId', render: (v: string) => <span className="worker-name"><ThunderboltOutlined />{v}</span> },
    { title: t('app.gateways.colVersion'), dataIndex: 'version', render: (v: string) => <Tag className="id-tag">{v}</Tag> },
    {
      title: t('app.gateways.colGrpc'),
      dataIndex: 'grpcAddress',
      render: (v: string) => <code className="address-code">{v}</code>,
    },
    {
      title: t('app.gateways.colDiscovery'),
      dataIndex: 'allowClientDiscovery',
      render: (v: boolean) => (v ? <Tag color="success">{t('app.gateways.allow')}</Tag> : <Tag color="warning">{t('app.gateways.deny')}</Tag>),
    },
    {
      title: t('app.gateways.colLoad'),
      key: 'load',
      render: (_, g) => {
        const load = loads[g.grpcAddress];
        return (
          <>
            <a onClick={() => queryLoad(g.grpcAddress)} style={{ marginRight: 10 }}>
              {t('app.gateways.query')}
            </a>
            {load &&
              (load.activeConnections >= 0 ? (
                <Tag color="geekblue">{t('app.gateways.connections', { count: load.activeConnections })}</Tag>
              ) : (
                <Tag color="error">{t('app.gateways.queryFailed')}</Tag>
              ))}
          </>
        );
      },
    },
  ];

  return (
    <div className="page">
      <PageHeader
        compact
        eyebrow="GATEWAY DISCOVERY"
        title={t('app.gateways.heroTitle')}
        description={t('app.gateways.heroDesc')}
        visual={<WorkerPulseVisual />}
      />

      {error && <Alert type="warning" showIcon message={t('app.common.partialError', { error })} style={{ marginBottom: 14 }} />}

      <section className="panel table-panel">
        <SectionHeader
          title={t('app.gateways.listTitle')}
          description={t('app.gateways.listDesc', { count: gateways.length })}
          extra={<Button size="small" icon={<SearchOutlined />} onClick={queryAll}>{t('app.gateways.queryAll')}</Button>}
        />
        <Table
          rowKey="grpcAddress"
          size="middle"
          loading={loading && !data}
          dataSource={gateways}
          columns={columns}
          pagination={false}
          locale={{ emptyText: <Empty description={t('app.gateways.empty')} /> }}
        />
      </section>

      <section className="panel">
        <SectionHeader title={t('app.gateways.loadTitle')} description={t('app.gateways.loadDesc')} />
        {hasKnownLoad ? (
          <EChart option={loadOption} height={230} />
        ) : (
          <Empty description={t('app.gateways.loadEmpty')} style={{ padding: '28px 0' }} />
        )}
      </section>
    </div>
  );
}
