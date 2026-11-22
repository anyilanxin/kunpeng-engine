import { useEffect, useMemo, useState } from 'react';
import { Button, Descriptions, Drawer, Empty, Input, Segmented, Select, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CloudServerOutlined,
  EyeOutlined,
  GatewayOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import MetricCard from '../components/MetricCard';
import NodeClusterVisual from '../components/NodeClusterVisual';
import PageHeader from '../components/PageHeader';
import SectionHeader from '../components/SectionHeader';
import { HealthBadge, RoleBadge } from '../components/StatusBadges';
import { useBrokers, useJobWorkers } from '../data/ClusterDataContext';
import { useMockRefresh } from '../hooks/useMockRefresh';
import type { Broker } from '../types/cluster';
import { getPartitionCells, shortNodeName } from '../utils/cluster';

type NodeTypeFilter = 'ALL' | 'BROKER' | 'GATEWAY';

function NodeTypeTags({ gateway }: { gateway: boolean }) {
  return <div className="node-role-tags"><span className="node-role-tag node-role-tag--broker"><CloudServerOutlined />Broker</span>{gateway && <span className="node-role-tag node-role-tag--gateway"><GatewayOutlined />Gateway</span>}</div>;
}

export default function NodesPage() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get('keyword') ?? '');
  const [health, setHealth] = useState('ALL');
  const [nodeType, setNodeType] = useState<NodeTypeFilter>('ALL');
  const [selectedNode, setSelectedNode] = useState<Broker | null>(null);
  const { refreshing } = useMockRefresh();
  const brokers = useBrokers();
  const workers = useJobWorkers();
  const allPartitions = getPartitionCells(brokers);

  const gatewayIds = useMemo(
    () => new Set(workers.map(({ gateway }) => gateway.memberId)),
    [workers],
  );
  const getNodeGateway = (memberId: string) => workers.find(({ gateway }) => gateway.memberId === memberId)?.gateway;

  useEffect(() => setKeyword(searchParams.get('keyword') ?? ''), [searchParams]);

  const nodes = useMemo(() => brokers.filter((broker) => {
    const matchesKeyword = `${broker.memberId} ${broker.address} ${broker.nodeSourceId}`.toLowerCase().includes(keyword.toLowerCase());
    const hasIssue = broker.partitions.some((partition) => partition.health !== 'HEALTHY');
    const matchesType = nodeType === 'ALL' || (nodeType === 'GATEWAY' ? gatewayIds.has(broker.memberId) : true);
    return matchesKeyword && matchesType && (health === 'ALL' || (health === 'ATTENTION' ? hasIssue : !hasIssue));
  }), [brokers, gatewayIds, health, keyword, nodeType]);
  const selectedGateway = selectedNode ? getNodeGateway(selectedNode.memberId) : undefined;

  const stateLabel = (state: string | undefined) =>
    state === 'OFFLINE' ? t('app.common.offline') : state === 'UNREGISTERED' ? t('app.common.unregistered') : t('app.common.online');

  const columns: ColumnsType<Broker> = [
    {
      title: t('app.nodes.colBroker'),
      dataIndex: 'memberId',
      width: 220,
      render: (memberId: string) => <div className="node-identity"><span className="node-identity__icon"><CloudServerOutlined /><i /></span><div><strong>{shortNodeName(memberId)}</strong><small>{memberId}</small></div></div>,
    },
    {
      title: t('app.nodes.colRole'),
      key: 'nodeType',
      width: 190,
      render: (_, broker) => <NodeTypeTags gateway={gatewayIds.has(broker.memberId)} />,
    },
    { title: t('app.nodes.colAddress'), dataIndex: 'address', width: 190, render: (address: string | null) => <code className="address-code">{address ?? '—'}</code> },
    {
      title: t('app.nodes.colState'),
      key: 'state',
      width: 96,
      render: (_, broker) => (
        <span className={`node-state-tag node-state-tag--${(broker.state ?? 'ONLINE').toLowerCase()}`}>
          <i />{stateLabel(broker.state)}
        </span>
      ),
    },
    { title: 'Node Source ID', dataIndex: 'nodeSourceId', width: 140, render: (id: number) => <Tag className="id-tag">#{id}</Tag> },
    {
      title: t('app.nodes.colLeaders'),
      key: 'leaders',
      width: 130,
      sorter: (a, b) => a.partitions.filter((item) => item.role === 'LEADER').length - b.partitions.filter((item) => item.role === 'LEADER').length,
      render: (_, broker) => <span className="number-cell number-cell--leader">{broker.partitions.filter((item) => item.role === 'LEADER').length}</span>,
    },
    {
      title: t('app.nodes.colFollowers'),
      key: 'followers',
      width: 140,
      render: (_, broker) => <span className="number-cell number-cell--follower">{broker.partitions.filter((item) => item.role === 'FOLLOWER').length}</span>,
    },
    {
      title: t('app.common.replicaHealth'),
      key: 'health',
      width: 160,
      render: (_, broker) => <HealthBadge health={broker.partitions.some((item) => item.health !== 'HEALTHY') ? 'UNHEALTHY' : 'HEALTHY'} />,
    },
    { title: t('app.common.action'), key: 'action', fixed: 'right', width: 100, render: (_, broker) => <Button type="link" icon={<EyeOutlined />} onClick={() => setSelectedNode(broker)}>{t('app.common.detail')}</Button> },
  ];

  return (
    <div className="page">
      <PageHeader
        compact
        eyebrow="BROKER TOPOLOGY"
        title={t('app.nodes.heroTitle')}
        description={t('app.nodes.heroDesc')}
        visual={<NodeClusterVisual />}
      />

      <section className="metrics-grid">
        <MetricCard title={t('app.common.metricOnlineBrokers')} value={<>{brokers.filter((broker) => broker.state === 'ONLINE').length}<small> / {brokers.length}</small></>} icon={<CloudServerOutlined />} tone="blue" footer={t('app.common.metricOnlineFooter', { online: brokers.filter((broker) => broker.state === 'ONLINE').length, total: brokers.length })} />
        <MetricCard title={t('app.nodes.metricBrokerNodes')} value={brokers.length} icon={<CloudServerOutlined />} tone="cyan" footer={t('app.nodes.metricBrokerFooter')} />
        <MetricCard title={t('app.nodes.metricGateways')} value={workers.length} icon={<GatewayOutlined />} tone="violet" footer={t('app.nodes.metricGatewayFooter')} />
        <MetricCard title={t('app.nodes.metricUnhealthy')} value={allPartitions.filter((item) => item.health !== 'HEALTHY').length} icon={<SafetyCertificateOutlined />} tone="green" footer={t('app.nodes.metricUnhealthyFooter')} />
      </section>

      <section className="panel table-panel">
        <SectionHeader
          title={t('app.nodes.listTitle')}
          description={t('app.nodes.listDesc', { count: nodes.length })}
          extra={<div className="table-filters"><Input allowClear prefix={<SearchOutlined />} placeholder={t('app.nodes.searchPlaceholder')} value={keyword} onChange={(event) => setKeyword(event.target.value)} /><Select value={health} onChange={setHealth} options={[{ value: 'ALL', label: t('app.nodes.healthAll') }, { value: 'HEALTHY', label: t('app.common.healthy') }, { value: 'ATTENTION', label: t('app.common.attention') }]} /></div>}
        />
        <div className="node-type-tabs">
          <Segmented
            value={nodeType}
            onChange={(value) => setNodeType(value as NodeTypeFilter)}
            options={[
              { value: 'ALL', label: t('app.nodes.tabAll', { count: brokers.length }) },
              { value: 'BROKER', label: t('app.nodes.tabBroker', { count: brokers.length }) },
              { value: 'GATEWAY', label: t('app.nodes.tabGateway', { count: workers.length }) },
            ]}
          />
          <span>{t('app.nodes.tabHint')}</span>
        </div>
        <Table
          rowKey="memberId"
          columns={columns}
          dataSource={nodes}
          loading={refreshing}
          pagination={false}
          scroll={{ x: 1210 }}
          locale={{ emptyText: <Empty description={t('app.common.noMatchingNodes')} /> }}
        />
        <div className="table-panel__footer"><span><i className="live-dot" /> {t('app.nodes.heartbeatOk')}</span><small>{t('app.nodes.lastUpdated', { time: t('app.header.justNow') })}</small></div>
      </section>

      <Drawer
        className="detail-drawer"
        title={selectedNode ? t('app.nodes.drawerTitle') : ''}
        width={580}
        open={Boolean(selectedNode)}
        onClose={() => setSelectedNode(null)}
        extra={selectedNode && <HealthBadge health={selectedNode.partitions.some((item) => item.health !== 'HEALTHY') ? 'UNHEALTHY' : 'HEALTHY'} />}
      >
        {selectedNode && (
          <>
            <div className="drawer-node-hero">
              <span className="drawer-node-hero__icon"><CloudServerOutlined /></span>
              <div><h3>{shortNodeName(selectedNode.memberId)}</h3><p>{selectedNode.memberId}</p></div>
              <span className="drawer-node-hero__signal"><i /><i /><i /></span>
            </div>
            <Descriptions column={2} size="middle" bordered items={[
              { key: 'address', label: t('app.nodes.colAddress'), children: selectedNode.address, span: 2 },
              { key: 'roles', label: t('app.nodes.labelRole'), children: <NodeTypeTags gateway={gatewayIds.has(selectedNode.memberId)} />, span: 2 },
              { key: 'source', label: 'Node Source ID', children: selectedNode.nodeSourceId },
              { key: 'count', label: t('app.nodes.labelReplicaCount'), children: selectedNode.partitions.length },
              { key: 'leaders', label: 'Leader', children: selectedNode.partitions.filter((item) => item.role === 'LEADER').length },
              { key: 'followers', label: 'Follower', children: selectedNode.partitions.filter((item) => item.role === 'FOLLOWER').length },
            ]} />
            {selectedGateway && (
              <>
                <div className="drawer-section-title"><strong>{t('app.nodes.gatewaySection')}</strong><span>{t('app.nodes.gatewaySessions', { count: selectedGateway.connections.reduce((sum, item) => sum + item.sessions.length, 0) })}</span></div>
                <div className="node-gateway-summary">
                  <div className="node-gateway-summary__status"><GatewayOutlined /><span><strong>{t('app.nodes.channelNormal')}</strong><small>{selectedGateway.address}</small></span><i className="live-dot" /></div>
                  <div className="node-gateway-summary__jobs">{selectedGateway.connections.map((connection) => <Tag key={connection.jobType}>{connection.jobType} · {connection.sessions.length}</Tag>)}</div>
                </div>
              </>
            )}
            <div className="drawer-section-title"><strong>{t('app.nodes.brokerSection')}</strong><span>{t('app.common.replicasUnit', { count: selectedNode.partitions.length })}</span></div>
            <Table
              rowKey={(record) => `${record.group}-${record.id}`}
              dataSource={selectedNode.partitions}
              size="small"
              pagination={false}
              scroll={{ y: 360 }}
              columns={[
                { title: t('app.nodes.colGroup'), dataIndex: 'group', render: (group: string) => <span className="partition-group-name">{group}</span> },
                { title: t('app.nodes.colId'), dataIndex: 'id', width: 56 },
                { title: t('app.nodes.partitionRoleCol'), dataIndex: 'role', width: 105, render: (role) => <RoleBadge role={role} /> },
                { title: t('app.common.healthy'), dataIndex: 'health', width: 90, render: (value) => <HealthBadge health={value} /> },
              ]}
            />
          </>
        )}
      </Drawer>
    </div>
  );
}
