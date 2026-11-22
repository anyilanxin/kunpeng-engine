import { useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { Alert, Button, Descriptions, Drawer, Empty, Input, Select, Tag } from 'antd';
import {
  CloudServerOutlined,
  DeploymentUnitOutlined,
  PartitionOutlined,
  ReloadOutlined,
  SearchOutlined,
  SwapOutlined,
} from '@ant-design/icons';
import MetricCard from './MetricCard';
import PageHeader from './PageHeader';
import SectionHeader from './SectionHeader';
import { HealthBadge, RoleBadge } from './StatusBadges';
import { useBrokers } from '../data/ClusterDataContext';
import { useMockRefresh } from '../hooks/useMockRefresh';
import type { PartitionCell, PartitionRole } from '../types/cluster';
import { getPartitionCells, shortNodeName } from '../utils/cluster';

const roleOptions: { value: 'ALL' | PartitionRole; label: string }[] = [
  { value: 'ALL', label: '全部角色' },
  { value: 'LEADER', label: 'Leader' },
  { value: 'FOLLOWER', label: 'Follower' },
  { value: 'CANDIDATE', label: 'Candidate' },
  { value: 'PROMOTABLE', label: 'Promotable' },
  { value: 'PASSIVE', label: 'Passive' },
  { value: 'INACTIVE', label: 'Inactive' },
];

interface PartitionMatrixPageProps {
  group: string;
  eyebrow: string;
  title: string;
  description: string;
  visual: ReactNode;
  refreshText: string;
}

export default function PartitionMatrixPage({ group, eyebrow, title, description, visual, refreshText }: PartitionMatrixPageProps) {
  const [role, setRole] = useState<'ALL' | PartitionRole>('ALL');
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState<PartitionCell | null>(null);
  const { refreshing, refresh } = useMockRefresh(refreshText);
  const brokers = useBrokers();
  const cells = useMemo(() => getPartitionCells(brokers, group), [brokers, group]);

  const partitionIds = useMemo(() => Array.from(new Set(
    cells.map((cell) => cell.id),
  )).sort((a, b) => a - b), [cells]);

  /** 节点卡片：每节点全部副本按角色 chips 铺排；网格固定 3 列 × 2 排可视（约 6 节点），超出滚动 */
  const brokerRows = useMemo(() => brokers.map((broker) => ({
    broker,
    partitions: broker.partitions
      .filter((partition) => partition.group === group && (role === 'ALL' || partition.role === role))
      .sort((a, b) => a.id - b.id),
  })).filter(({ broker, partitions }) => {
    const haystack = `${broker.memberId} ${broker.address} ${broker.partitions.filter((p) => p.group === group).map((p) => `#${p.id}`).join(' ')}`.toLowerCase();
    return haystack.includes(keyword.toLowerCase()) && partitions.length > 0;
  }), [brokers, group, role, keyword]);

  const replicationFactor = partitionIds.length
    ? Math.round(cells.length / partitionIds.length)
    : 0;

  const selectedPeers = selected ? cells.filter((cell) => cell.id === selected.id) : [];
  const leader = selectedPeers.find((cell) => cell.role === 'LEADER');
  const followers = selectedPeers.filter((cell) => cell.role !== 'LEADER');

  return (
    <div className="page">
      <PageHeader
        compact
        eyebrow={eyebrow}
        title={title}
        description={description}
        visual={visual}
        actions={<Button icon={<ReloadOutlined spin={refreshing} />} onClick={refresh}>刷新分区快照</Button>}
      />

      <section className="metrics-grid">
        <MetricCard title="在线 Broker" value={brokers.length} icon={<CloudServerOutlined />} tone="green" footer={`${brokers.length} 个节点在线`} />
        <MetricCard title="逻辑分区" value={partitionIds.length} icon={<PartitionOutlined />} tone="blue" footer="1 个分区组" />
        <MetricCard title="副本因子" value={replicationFactor} icon={<DeploymentUnitOutlined />} tone="cyan" footer={`共 ${cells.length} 个分区副本`} />
        <MetricCard title="当前变更" value="无" icon={<SwapOutlined />} tone="violet" footer="以当前快照为准" />
      </section>

      <section className={`panel partition-panel ${refreshing ? 'is-refreshing' : ''}`}>
        <SectionHeader
          title="分区归属"
          description="一张卡片一个节点，卡片内为该节点承载的全部分区副本（按角色着色）"
          extra={<span className="snapshot-label"><i className="live-dot" /> 快照已同步</span>}
        />
        <div className="partition-toolbar">
          <div className="partition-toolbar__left">
            <Input allowClear value={keyword} onChange={(event) => setKeyword(event.target.value)} prefix={<SearchOutlined />} placeholder="搜索节点或分区号" />
            <Select value={role} onChange={setRole} options={roleOptions} />
            <span className="matrix-count">{brokerRows.length}{brokerRows.length !== brokers.length ? ` / ${brokers.length}` : ''} 个节点 · {partitionIds.length} 个分区</span>
          </div>
          <div className="matrix-legend">
            {(['LEADER', 'FOLLOWER', 'CANDIDATE', 'PROMOTABLE', 'PASSIVE', 'INACTIVE'] as PartitionRole[]).map((item) => (
              <button key={item} className={`${role === item ? 'active' : ''}`} onClick={() => setRole(role === item ? 'ALL' : item)}><RoleBadge role={item} /></button>
            ))}
          </div>
        </div>

        {brokerRows.length === 0 ? (
          <Empty description="没有符合条件的节点" />
        ) : (
          <div className="partition-node-grid">
            {brokerRows.map(({ broker, partitions }) => (
              <div key={broker.memberId} className="partition-node-card">
                <div className="partition-node-card__header">
                  <div className="node-identity">
                    <span className="node-identity__icon"><CloudServerOutlined /><i /></span>
                    <div><strong>{shortNodeName(broker.memberId)}</strong><small>{broker.memberId} · Source #{broker.nodeSourceId}</small></div>
                  </div>
                  <span className="partition-node-card__count">{partitions.length} 个副本</span>
                </div>
                <div className="partition-node-card__meta">{broker.address}</div>
                <div className="node-partition-chips">
                  {partitions.map((partition) => (
                    <button
                      key={partition.id}
                      className={`node-partition-chip node-partition-chip--${partition.role.toLowerCase()} ${partition.health !== 'HEALTHY' ? 'node-partition-chip--unhealthy' : ''}`}
                      onClick={() => setSelected({ ...partition, broker })}
                      title={`${partition.group}-${partition.id} · ${partition.role} · ${partition.health} · Source #${partition.sourceId}`}
                    >
                      <b>#{partition.id}</b>
                      <span>{partition.role.charAt(0) + partition.role.slice(1).toLowerCase()}</span>
                      <i className="partition-cell__health" />
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
        <div className="matrix-footer"><span>一张卡片一个节点，chips 颜色按角色（Leader 蓝 / Follower 灰蓝 / Candidate 橙 / Promotable 紫），红色为异常副本；节点多于 6 个或卡内分区多于 3 排时区域内滚动。</span><span>点击分区 chip 查看复制链路 →</span></div>
      </section>

      <Drawer
        className="detail-drawer partition-detail-drawer"
        title={selected ? `${selected.group} · 分区 ${selected.id}` : ''}
        width={560}
        open={Boolean(selected)}
        onClose={() => setSelected(null)}
        extra={selected && <RoleBadge role={selected.role} />}
      >
        {selected && (
          <>
            {selected.health !== 'HEALTHY' && <Alert className="partition-alert" type="warning" showIcon message="当前副本健康状态异常" description="建议检查节点网络与 Raft 日志复制状态。" />}
            <div className="partition-detail-identity">
              <span><PartitionOutlined /></span>
              <div><small>PARTITION</small><strong>{selected.group}-{selected.id}</strong><p>{selected.broker.memberId} · {selected.broker.address}</p></div>
              <HealthBadge health={selected.health} />
            </div>
            <Descriptions column={2} bordered size="middle" items={[
              { key: 'group', label: '分区组', children: selected.group, span: 2 },
              { key: 'id', label: '分区 ID', children: selected.id },
              { key: 'term', label: '当前任期', children: <Tag color="blue">Term {selected.term}</Tag> },
              { key: 'source', label: 'Source ID', children: selected.sourceId },
              { key: 'node', label: 'Node Source ID', children: selected.broker.nodeSourceId },
              { key: 'agents', label: 'Agent Source IDs', children: selected.agentSourceIds.length ? selected.agentSourceIds.map((id) => <Tag key={id}>#{id}</Tag>) : '—', span: 2 },
            ]} />
            <div className="drawer-section-title"><strong>Raft 副本复制链路</strong><span>Term {selected.term}</span></div>
            <div className="replication-flow">
              {leader && <div className="replication-leader"><span className="replication-node-icon"><DeploymentUnitOutlined /></span><div><small>LEADER</small><strong>{shortNodeName(leader.broker.memberId)}</strong></div><i className="live-dot" /></div>}
              <div className="replication-stream"><span /><i /><i /><i /></div>
              <div className="replication-followers">
                {followers.map((cell) => <div key={cell.broker.memberId}><span className="replication-node-icon"><DeploymentUnitOutlined /></span><div><small>{cell.role}</small><strong>{shortNodeName(cell.broker.memberId)}</strong></div><HealthBadge health={cell.health} /></div>)}
              </div>
              <p><span className="live-dot" /> 日志条目正在从 Leader 复制到 {followers.length} 个副本</p>
            </div>
          </>
        )}
      </Drawer>
    </div>
  );
}
