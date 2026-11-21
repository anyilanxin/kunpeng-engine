import { useEffect, useMemo, useState } from 'react';
import { Button, Descriptions, Drawer, Empty, Input, Segmented, Select, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CloudServerOutlined,
  EyeOutlined,
  GatewayOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
} from '@ant-design/icons';
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
  const [searchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get('keyword') ?? '');
  const [health, setHealth] = useState('ALL');
  const [nodeType, setNodeType] = useState<NodeTypeFilter>('ALL');
  const [selectedNode, setSelectedNode] = useState<Broker | null>(null);
  const { refreshing, refresh } = useMockRefresh('节点状态已刷新');
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

  const columns: ColumnsType<Broker> = [
    {
      title: 'Broker 节点',
      dataIndex: 'memberId',
      width: 220,
      render: (memberId: string) => <div className="node-identity"><span className="node-identity__icon"><CloudServerOutlined /><i /></span><div><strong>{shortNodeName(memberId)}</strong><small>{memberId}</small></div></div>,
    },
    {
      title: '节点角色',
      key: 'nodeType',
      width: 190,
      render: (_, broker) => <NodeTypeTags gateway={gatewayIds.has(broker.memberId)} />,
    },
    { title: '服务地址', dataIndex: 'address', width: 190, render: (address: string) => <code className="address-code">{address}</code> },
    { title: 'Node Source ID', dataIndex: 'nodeSourceId', width: 140, render: (id: number) => <Tag className="id-tag">#{id}</Tag> },
    {
      title: 'Leader 分区',
      key: 'leaders',
      width: 130,
      sorter: (a, b) => a.partitions.filter((item) => item.role === 'LEADER').length - b.partitions.filter((item) => item.role === 'LEADER').length,
      render: (_, broker) => <span className="number-cell number-cell--leader">{broker.partitions.filter((item) => item.role === 'LEADER').length}</span>,
    },
    {
      title: 'Follower 分区',
      key: 'followers',
      width: 140,
      render: (_, broker) => <span className="number-cell number-cell--follower">{broker.partitions.filter((item) => item.role === 'FOLLOWER').length}</span>,
    },
    {
      title: '副本健康',
      key: 'health',
      width: 160,
      render: (_, broker) => <HealthBadge health={broker.partitions.some((item) => item.health !== 'HEALTHY') ? 'UNHEALTHY' : 'HEALTHY'} />,
    },
    { title: '操作', key: 'action', fixed: 'right', width: 100, render: (_, broker) => <Button type="link" icon={<EyeOutlined />} onClick={() => setSelectedNode(broker)}>详情</Button> },
  ];

  return (
    <div className="page">
      <PageHeader
        compact
        eyebrow="BROKER TOPOLOGY"
        title="每一个节点，都清晰可见"
        description="统一查看 Broker 与 Gateway 节点，追踪连接状态、分区角色和副本健康。"
        visual={<NodeClusterVisual />}
        actions={<Button icon={<ReloadOutlined spin={refreshing} />} onClick={refresh}>刷新节点状态</Button>}
      />

      <section className="metrics-grid">
        <MetricCard title="在线 Broker" value={<>{brokers.length}<small> / {brokers.length}</small></>} icon={<CloudServerOutlined />} tone="blue" footer={`${brokers.length} 个节点在线`} />
        <MetricCard title="Broker 节点" value={brokers.length} icon={<CloudServerOutlined />} tone="cyan" footer="全部承载 Raft 分区" />
        <MetricCard title="Gateway 节点" value={workers.length} icon={<GatewayOutlined />} tone="violet" footer="Worker 连接状态正常" />
        <MetricCard title="异常副本" value={allPartitions.filter((item) => item.health !== 'HEALTHY').length} icon={<SafetyCertificateOutlined />} tone="green" footer="需要关注的副本数量" />
      </section>

      <section className="panel table-panel">
        <SectionHeader
          title="集群节点列表"
          description={`共 ${nodes.length} 个节点 · 数据来自集群实时快照`}
          extra={<div className="table-filters"><Input allowClear prefix={<SearchOutlined />} placeholder="搜索节点或地址" value={keyword} onChange={(event) => setKeyword(event.target.value)} /><Select value={health} onChange={setHealth} options={[{ value: 'ALL', label: '全部健康状态' }, { value: 'HEALTHY', label: '健康' }, { value: 'ATTENTION', label: '需要关注' }]} /></div>}
        />
        <div className="node-type-tabs">
          <Segmented
            value={nodeType}
            onChange={(value) => setNodeType(value as NodeTypeFilter)}
            options={[
              { value: 'ALL', label: `全部节点 ${brokers.length}` },
              { value: 'BROKER', label: `Broker 节点 ${brokers.length}` },
              { value: 'GATEWAY', label: `Gateway 节点 ${workers.length}` },
            ]}
          />
          <span>同一节点可同时承担 Broker 与 Gateway 角色</span>
        </div>
        <Table
          rowKey="memberId"
          columns={columns}
          dataSource={nodes}
          loading={refreshing}
          pagination={false}
          scroll={{ x: 1210 }}
          locale={{ emptyText: <Empty description="没有符合条件的节点" /> }}
        />
        <div className="table-panel__footer"><span><i className="live-dot" /> 节点心跳正常</span><small>最后更新：刚刚</small></div>
      </section>

      <Drawer
        className="detail-drawer"
        title={selectedNode ? '集群节点详情' : ''}
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
              { key: 'address', label: '服务地址', children: selectedNode.address, span: 2 },
              { key: 'roles', label: '节点角色', children: <NodeTypeTags gateway={gatewayIds.has(selectedNode.memberId)} />, span: 2 },
              { key: 'source', label: 'Node Source ID', children: selectedNode.nodeSourceId },
              { key: 'count', label: '分区副本数', children: selectedNode.partitions.length },
              { key: 'leaders', label: 'Leader', children: selectedNode.partitions.filter((item) => item.role === 'LEADER').length },
              { key: 'followers', label: 'Follower', children: selectedNode.partitions.filter((item) => item.role === 'FOLLOWER').length },
            ]} />
            {selectedGateway && (
              <>
                <div className="drawer-section-title"><strong>Gateway 连接</strong><span>{selectedGateway.connections.reduce((sum, item) => sum + item.sessions.length, 0)} 个 Worker 会话</span></div>
                <div className="node-gateway-summary">
                  <div className="node-gateway-summary__status"><GatewayOutlined /><span><strong>任务订阅通道正常</strong><small>{selectedGateway.address}</small></span><i className="live-dot" /></div>
                  <div className="node-gateway-summary__jobs">{selectedGateway.connections.map((connection) => <Tag key={connection.jobType}>{connection.jobType} · {connection.sessions.length}</Tag>)}</div>
                </div>
              </>
            )}
            <div className="drawer-section-title"><strong>Broker 分区</strong><span>{selectedNode.partitions.length} 个副本</span></div>
            <Table
              rowKey={(record) => `${record.group}-${record.id}`}
              dataSource={selectedNode.partitions}
              size="small"
              pagination={false}
              scroll={{ y: 360 }}
              columns={[
                { title: '分区组', dataIndex: 'group', render: (group: string) => <span className="partition-group-name">{group}</span> },
                { title: 'ID', dataIndex: 'id', width: 56 },
                { title: '角色', dataIndex: 'role', width: 105, render: (role) => <RoleBadge role={role} /> },
                { title: '健康', dataIndex: 'health', width: 90, render: (value) => <HealthBadge health={value} /> },
              ]}
            />
          </>
        )}
      </Drawer>
    </div>
  );
}
