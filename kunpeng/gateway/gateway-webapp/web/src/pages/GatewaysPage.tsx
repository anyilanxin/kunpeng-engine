import { useMemo, useState } from 'react';
import { SearchOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { Alert, Button, Empty, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { api } from '../api';
import { usePolling } from '../usePolling';
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

  const loadOption = useMemo(() => {
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
        axisLine: { lineStyle: { color: '#dbe6f5' } },
        axisTick: { show: false },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        splitLine: { lineStyle: { color: '#e8eef8' } },
      },
      series: [
        {
          name: '活跃连接',
          type: 'bar',
          barWidth: 26,
          itemStyle: { borderRadius: [6, 6, 0, 0], color: '#246bfd' },
          label: { show: true, position: 'top', color: '#51637e' },
          data: entries.map((e) => (e.known && e.value >= 0 ? e.value : null)),
        },
      ],
    };
  }, [gateways, loads]);

  const columns: ColumnsType<GatewayVo> = [
    { title: '节点', dataIndex: 'nodeId', render: (v: string) => <span className="worker-name"><ThunderboltOutlined />{v}</span> },
    { title: '版本', dataIndex: 'version', render: (v: string) => <Tag className="id-tag">{v}</Tag> },
    {
      title: 'gRPC 接入地址',
      dataIndex: 'grpcAddress',
      render: (v: string) => <code className="address-code">{v}</code>,
    },
    {
      title: '客户端发现',
      dataIndex: 'allowClientDiscovery',
      render: (v: boolean) => (v ? <Tag color="success">允许</Tag> : <Tag color="warning">关闭</Tag>),
    },
    {
      title: '活跃连接',
      key: 'load',
      render: (_, g) => {
        const load = loads[g.grpcAddress];
        return (
          <>
            <a onClick={() => queryLoad(g.grpcAddress)} style={{ marginRight: 10 }}>
              查询
            </a>
            {load &&
              (load.activeConnections >= 0 ? (
                <Tag color="geekblue">{load.activeConnections} 连接</Tag>
              ) : (
                <Tag color="error">查询失败</Tag>
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
        title="网关接入，一览无余"
        description="查看网关发现列表与客户端连接负载，网关无状态多副本水平扩展。"
        visual={<WorkerPulseVisual />}
      />

      {error && <Alert type="warning" showIcon message={`部分数据拉取失败：${error}`} style={{ marginBottom: 14 }} />}

      <section className="panel table-panel">
        <SectionHeader
          title="网关发现列表"
          description={`共 ${gateways.length} 个网关 · 由集群拓扑通告`}
          extra={<Button size="small" icon={<SearchOutlined />} onClick={queryAll}>查询全部负载</Button>}
        />
        <Table
          rowKey="grpcAddress"
          size="middle"
          loading={loading && !data}
          dataSource={gateways}
          columns={columns}
          pagination={false}
          locale={{ emptyText: <Empty description="暂无网关" /> }}
        />
      </section>

      <section className="panel">
        <SectionHeader title="网关负载" description="活跃客户端连接数（点击表格「查询」后更新）" />
        {hasKnownLoad ? (
          <EChart option={loadOption} height={230} />
        ) : (
          <Empty description="尚未查询负载，点击表格中的「查询」或「查询全部负载」后展示" style={{ padding: '28px 0' }} />
        )}
      </section>
    </div>
  );
}
