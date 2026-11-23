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
import { useLayoutEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { Alert, Descriptions, Drawer, Empty, Input, Select, Tag } from 'antd';
import {
  CloudServerOutlined,
  DeploymentUnitOutlined,
  PartitionOutlined,
  SearchOutlined,
  SwapOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import MetricCard from './MetricCard';
import PageHeader from './PageHeader';
import SectionHeader from './SectionHeader';
import { HealthBadge, RoleBadge } from './StatusBadges';
import { useBrokers } from '../data/ClusterDataContext';
import { useMockRefresh } from '../hooks/useMockRefresh';
import type { PartitionCell, PartitionRole } from '../types/cluster';
import { getPartitionCells, shortNodeName } from '../utils/cluster';

const roleOptions = (t: (key: string) => string): { value: 'ALL' | PartitionRole; label: string }[] => [
  { value: 'ALL', label: t('app.partitions.allRoles') },
  { value: 'LEADER', label: 'Leader' },
  { value: 'FOLLOWER', label: 'Follower' },
  { value: 'CANDIDATE', label: 'Candidate' },
  { value: 'PROMOTABLE', label: 'Promotable' },
  { value: 'PASSIVE', label: 'Passive' },
  { value: 'INACTIVE', label: 'Inactive' },
];

/** Raft 副本复制链路：节点卡片保持原样式（Leader/副本卡片），Leader 到每个副本的具体流向用流线动画（参照集群信息拓扑）；当前查看节点紫色标记 */
function ReplicationFlow({
  leader,
  followers,
  viewingMemberId,
}: {
  leader: PartitionCell | undefined;
  followers: PartitionCell[];
  viewingMemberId: string;
}) {
  const wrapRef = useRef<HTMLDivElement>(null);
  const leaderRef = useRef<HTMLDivElement>(null);
  const followerRefs = useRef<(HTMLDivElement | null)[]>([]);
  const [routes, setRoutes] = useState<{ key: string; healthy: boolean; d: string }[]>([]);

  const isViewing = (memberId: string) => memberId === viewingMemberId;

  /** 卡片布局测量：Leader 右缘中点 -> 各副本左缘中点的贝塞尔路径（容器内相对坐标，Drawer 滑入变换被相对计算抵消） */
  useLayoutEffect(() => {
    const measure = () => {
      const wrap = wrapRef.current;
      const leaderCard = leaderRef.current;
      if (!wrap || !leaderCard) return;
      const wrapRect = wrap.getBoundingClientRect();
      const leaderRect = leaderCard.getBoundingClientRect();
      const x1 = leaderRect.right - wrapRect.left;
      const y1 = leaderRect.top + leaderRect.height / 2 - wrapRect.top;
      const measured = followers
        .map((cell, index) => {
          const el = followerRefs.current[index];
          if (!el) return null;
          const rect = el.getBoundingClientRect();
          const x2 = rect.left - wrapRect.left;
          const y2 = rect.top + rect.height / 2 - wrapRect.top;
          const dx = Math.max(30, (x2 - x1) * 0.5);
          return {
            key: cell.broker.memberId,
            healthy: cell.health === 'HEALTHY',
            d: `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`,
          };
        })
        .filter((route): route is { key: string; healthy: boolean; d: string } => route !== null);
      setRoutes(measured);
    };
    measure();
    const observer = new ResizeObserver(measure);
    if (wrapRef.current) observer.observe(wrapRef.current);
    return () => observer.disconnect();
  }, [leader, followers]);

  if (!leader) {
    // 无 Leader（选举中等）：无复制流向可画，节点平铺事实呈现
    return (
      <div className="rf-noleader">
        {[...followers].map((cell) => (
          <span key={cell.broker.memberId} className={`rf-noleader__chip ${isViewing(cell.broker.memberId) ? 'rf-noleader__chip--viewing' : ''}`}>
            <i />{shortNodeName(cell.broker.memberId)} · {cell.role}
          </span>
        ))}
      </div>
    );
  }

  return (
    <div className="rf-flow" ref={wrapRef}>
      <svg className="rf-lines" aria-hidden="true">
        <defs>
          <linearGradient id="rf-flow-line" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0" stopColor="#78c8ff" />
            <stop offset="1" stopColor="#3778f6" />
          </linearGradient>
          {routes.map((route) => (
            <path key={route.key} id={`rf-route-${route.key}`} d={route.d} className={route.healthy ? undefined : 'rf-route--unhealthy'} />
          ))}
        </defs>
        <g className="rf-links">{routes.map((route) => <use key={route.key} href={`#rf-route-${route.key}`} />)}</g>
        <g className="rf-flows">{routes.filter((route) => route.healthy).map((route) => <use key={route.key} href={`#rf-route-${route.key}`} />)}</g>
        {routes.filter((route) => route.healthy).map((route, index) => (
          <circle key={route.key} className="rf-particle" r="3.5">
            <animateMotion dur={`${2.1 + index * 0.15}s`} begin={`${index * 0.55}s`} repeatCount="indefinite">
              <mpath href={`#rf-route-${route.key}`} />
            </animateMotion>
          </circle>
        ))}
      </svg>

      {/* Leader 卡片（原样式，垂直居中于左列） */}
      <div className="rf-col rf-col--leader">
        <div ref={leaderRef} className={`replication-leader rf-card ${isViewing(leader.broker.memberId) ? 'rf-card--viewing' : ''}`}>
          <span className="replication-node-icon"><DeploymentUnitOutlined /></span>
          <div><small>LEADER</small><strong>{shortNodeName(leader.broker.memberId)}</strong></div>
          <i className="live-dot" />
        </div>
      </div>

      {/* 副本卡片（原样式，右列竖排；连线流向 = Leader → 副本） */}
      <div className="rf-col rf-col--followers replication-followers">
        {followers.map((cell, index) => (
          <div
            key={cell.broker.memberId}
            ref={(el) => { followerRefs.current[index] = el; }}
            className={`rf-card ${isViewing(cell.broker.memberId) ? 'rf-card--viewing' : ''}`}
          >
            <span className="replication-node-icon"><DeploymentUnitOutlined /></span>
            <div><small>{cell.role}</small><strong>{shortNodeName(cell.broker.memberId)}</strong></div>
            <HealthBadge health={cell.health} />
          </div>
        ))}
      </div>
    </div>
  );
}

interface PartitionMatrixPageProps {
  group: string;
  eyebrow: string;
  titleKey: string;
  descriptionKey: string;
  visual: ReactNode;
}

export default function PartitionMatrixPage({ group, eyebrow, titleKey, descriptionKey, visual }: PartitionMatrixPageProps) {
  const { t } = useTranslation();
  const [role, setRole] = useState<'ALL' | PartitionRole>('ALL');
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState<PartitionCell | null>(null);
  const { refreshing } = useMockRefresh();
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

  const stateLabel = (state: string | undefined) =>
    state === 'OFFLINE' ? t('app.common.offline') : state === 'UNREGISTERED' ? t('app.common.unregistered') : t('app.common.online');

  return (
    <div className="page">
      <PageHeader
        compact
        eyebrow={eyebrow}
        title={t(titleKey)}
        description={t(descriptionKey)}
        visual={visual}
      />

      <section className="metrics-grid">
        <MetricCard title={t('app.common.metricOnlineBrokers')} value={brokers.filter((broker) => broker.state !== 'OFFLINE').length} icon={<CloudServerOutlined />} tone="green" footer={t('app.common.metricOnlineFooter', { online: brokers.filter((broker) => broker.state === 'ONLINE').length, total: brokers.length })} />
        <MetricCard title={t('app.common.metricLogical')} value={partitionIds.length} icon={<PartitionOutlined />} tone="blue" footer={t('app.common.partitionGroupCount')} />
        <MetricCard title={t('app.partitions.replicationFactor')} value={replicationFactor} icon={<DeploymentUnitOutlined />} tone="cyan" footer={t('app.partitions.replicationFooter', { count: cells.length })} />
        <MetricCard title={t('app.common.currentChange')} value={t('app.common.none')} icon={<SwapOutlined />} tone="violet" footer={t('app.partitions.footerNote')} />
      </section>

      <section className={`panel partition-panel ${refreshing ? 'is-refreshing' : ''}`}>
        <SectionHeader
          title={t('app.partitions.sectionTitle')}
          description={t('app.partitions.sectionDesc')}
          extra={<span className="snapshot-label"><i className="live-dot" /> {t('app.partitions.snapshotSynced')}</span>}
        />
        <div className="partition-toolbar">
          <div className="partition-toolbar__left">
            <Input allowClear value={keyword} onChange={(event) => setKeyword(event.target.value)} prefix={<SearchOutlined />} placeholder={t('app.partitions.searchPlaceholder')} />
            <Select value={role} onChange={setRole} options={roleOptions(t)} />
            <span className="matrix-count">{t('app.partitions.nodeCount', { nodes: `${brokerRows.length}${brokerRows.length !== brokers.length ? ` / ${brokers.length}` : ''}`, partitions: partitionIds.length })}</span>
          </div>
          <div className="matrix-legend">
            {(['LEADER', 'FOLLOWER', 'CANDIDATE', 'PROMOTABLE', 'PASSIVE', 'INACTIVE'] as PartitionRole[]).map((item) => (
              <button key={item} className={`${role === item ? 'active' : ''}`} onClick={() => setRole(role === item ? 'ALL' : item)}><RoleBadge role={item} /></button>
            ))}
          </div>
        </div>

        {brokerRows.length === 0 ? (
          <Empty description={t('app.common.noMatchingNodes')} />
        ) : (
          <div className="partition-node-grid">
            {brokerRows.map(({ broker, partitions }) => (
              <div key={broker.memberId} className="partition-node-card">
                <div className="partition-node-card__header">
                  <div className="node-identity">
                    <span className="node-identity__icon"><CloudServerOutlined /><i /></span>
                    <div><strong>{shortNodeName(broker.memberId)}</strong><small>{broker.memberId} · Source #{broker.nodeSourceId}</small></div>
                  </div>
                  <span className={`node-state-tag node-state-tag--${(broker.state ?? 'ONLINE').toLowerCase()}`}>
                    <i />{stateLabel(broker.state)}
                  </span>
                  <span className="partition-node-card__count">{t('app.common.replicasUnit', { count: partitions.length })}</span>
                </div>
                <div className="partition-node-card__meta">{broker.address ?? '—'}</div>
                <div className="node-partition-chips">
                  {partitions.map((partition) => (
                    <button
                      key={partition.id}
                      className={`node-partition-chip node-partition-chip--${partition.role.toLowerCase()} ${partition.health !== 'HEALTHY' ? 'node-partition-chip--unhealthy' : ''}`}
                      onClick={() => setSelected({ ...partition, broker })}
                      title={`${partition.group}-${partition.id} · ${partition.role} · ${partition.health} · Source #${partition.sourceId}`}
                    >
                      <b>{partition.id}</b>
                      <span>{partition.role.charAt(0) + partition.role.slice(1).toLowerCase()}</span>
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
        <div className="matrix-footer"><span>{t('app.partitions.gridFooter')}</span><span>{t('app.partitions.gridFooterHint')}</span></div>
      </section>

      <Drawer
        className="detail-drawer partition-detail-drawer"
        title={selected ? t('app.partitions.drawerTitle', { group: selected.group, id: selected.id }) : ''}
        width={560}
        open={Boolean(selected)}
        onClose={() => setSelected(null)}
        extra={selected && <RoleBadge role={selected.role} />}
      >
        {selected && (
          <>
            {selected.health !== 'HEALTHY' && <Alert className="partition-alert" type="warning" showIcon message={t('app.partitions.healthAlert')} description={t('app.partitions.healthAlertDesc')} />}
            <div className="partition-detail-identity">
              <span><PartitionOutlined /></span>
              <div><small>PARTITION</small><strong>{selected.group}-{selected.id}</strong><p>{selected.broker.memberId} · {selected.broker.address}</p></div>
              <HealthBadge health={selected.health} />
            </div>
            <Descriptions column={2} bordered size="middle" items={[
              { key: 'group', label: t('app.partitions.labelGroup'), children: selected.group, span: 2 },
              { key: 'id', label: t('app.partitions.labelPartitionId'), children: selected.id },
              { key: 'term', label: t('app.partitions.labelTerm'), children: <Tag color="blue">Term {selected.term}</Tag> },
              { key: 'source', label: 'Source ID', children: selected.sourceId },
              { key: 'node', label: 'Node Source ID', children: selected.broker.nodeSourceId },
              { key: 'agents', label: t('app.partitions.labelAgents'), children: selected.agentSourceIds.length ? selected.agentSourceIds.map((id) => <Tag key={id}>#{id}</Tag>) : '—', span: 2 },
            ]} />
            <div className="drawer-section-title"><strong>{t('app.partitions.copySection')}</strong><span>Term {selected.term}</span></div>
            <div className="replication-flow">
              <ReplicationFlow leader={leader} followers={followers} viewingMemberId={selected.broker.memberId} />
              <p><span className="live-dot" /> {t('app.partitions.copyHint', { count: followers.length })}</p>
            </div>
          </>
        )}
      </Drawer>
    </div>
  );
}
