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
import { Button, Progress, Tag } from 'antd';
import {
  ApiOutlined,
  ArrowRightOutlined,
  CheckCircleFilled,
  CloudServerOutlined,
  PartitionOutlined,
  SyncOutlined,
  ThunderboltOutlined,
  WarningFilled,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import ClusterTopology from '../components/ClusterTopology';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionHeader from '../components/SectionHeader';
import PartitionGroupOverview from '../components/PartitionGroupOverview';
import { useBrokers, useJobWorkers } from '../data/ClusterDataContext';
import { getPartitionCells, getWorkerSessions, shortNodeName } from '../utils/cluster';

export default function DashboardPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const brokers = useBrokers();
  const workers = useJobWorkers();
  const partitions = getPartitionCells(brokers);
  const sessions = getWorkerSessions(workers);
  const jobTypes = new Set(sessions.map((session) => session.jobType));
  const leaders = partitions.filter((partition) => partition.role === 'LEADER').length;
  const followers = partitions.filter((partition) => partition.role === 'FOLLOWER').length;
  const healthy = partitions.filter((partition) => partition.health === 'HEALTHY').length;
  const unhealthy = partitions.filter((partition) => partition.health !== 'HEALTHY');
  const healthPercent = partitions.length ? Math.round((healthy / partitions.length) * 100) : 100;

  return (
    <div className="page page--dashboard">
      <PageHeader
        eyebrow="DISTRIBUTED WORKFLOW ENGINE"
        title={t('app.dashboard.heroTitle')}
        description={t('app.dashboard.heroDesc')}
        visual={<ClusterTopology />}
      />

      <section className="metrics-grid">
        <MetricCard title={t('app.common.metricOnlineBrokers')} value={<>{brokers.filter((broker) => broker.state !== 'OFFLINE').length}<small> / {brokers.length}</small></>} icon={<CloudServerOutlined />} tone="blue" footer={t('app.common.metricOnlineFooter', { online: brokers.filter((broker) => broker.state === 'ONLINE').length, total: brokers.length })} trend={0} />
        <MetricCard title={t('app.common.metricLogical')} value={new Set(partitions.map((item) => `${item.group}-${item.id}`)).size} icon={<PartitionOutlined />} tone="cyan" footer={t('app.dashboard.metricLeaderFollower', { leaders, followers })} />
        <MetricCard title={t('app.common.metricSessions')} value={sessions.length} icon={<ApiOutlined />} tone="violet" footer={t('app.dashboard.metricSessionTypes', { count: jobTypes.size })} />
        <MetricCard title={t('app.common.metricJobTypes')} value={jobTypes.size} icon={<ThunderboltOutlined />} tone="green" footer={t('app.common.metricJobTypesFooter')} />
      </section>

      <section className="dashboard-primary-grid">
        <PartitionGroupOverview />

        <div className="panel cluster-health-panel">
          <SectionHeader title={t('app.dashboard.healthTitle')} description={t('app.dashboard.healthDesc')} extra={<Tag color={unhealthy.length ? 'warning' : 'success'} className="soft-tag">{unhealthy.length ? t('app.common.attention') : t('app.dashboard.stable')}</Tag>} />
          <div className="health-score">
            <div className="health-donut">
              <svg viewBox="0 0 120 120">
                <circle className="health-donut__track" cx="60" cy="60" r="50" pathLength="100" />
                <circle className="health-donut__value" cx="60" cy="60" r="50" pathLength="100" strokeDasharray={`${healthPercent} 100`} />
              </svg>
              <div><strong>{healthPercent}<small>%</small></strong><span>{t('app.dashboard.healthScore')}</span></div>
            </div>
            <div className="health-score__legend">
              <div><span><i className="dot dot--blue" />{t('app.dashboard.healthyReplicas')}</span><strong>{healthy}</strong></div>
              <div><span><i className="dot dot--red" />{t('app.dashboard.unhealthyReplicas')}</span><strong className="danger-text">{partitions.length - healthy}</strong></div>
              <div><span><i className="dot dot--gray" />{t('app.common.currentChange')}</span><strong>{t('app.common.none')}</strong></div>
            </div>
          </div>
          <div className="health-role-bars">
            <div><span>Leader <b>{leaders}</b></span><Progress percent={partitions.length ? (leaders / partitions.length) * 100 : 0} showInfo={false} strokeColor="#2f6ff4" trailColor="var(--line-soft)" size="small" /></div>
            <div><span>Follower <b>{followers}</b></span><Progress percent={partitions.length ? (followers / partitions.length) * 100 : 0} showInfo={false} strokeColor="#8fb6f8" trailColor="var(--line-soft)" size="small" /></div>
          </div>
          {unhealthy.length > 0 && (
            <button className="health-warning" onClick={() => navigate('/partitions')}>
              <WarningFilled /> {unhealthy.slice(0, 2).map((item) => `${item.group}-${item.id}`).join('、')}{unhealthy.length > 2 ? t('app.dashboard.warningMore', { count: unhealthy.length }) : ''}{t('app.dashboard.warningSuffix')} <ArrowRightOutlined />
            </button>
          )}
        </div>
      </section>

      <section className="dashboard-secondary-grid">
        <div className="panel broker-overview-panel">
          <SectionHeader title={t('app.dashboard.brokerSection')} description={t('app.dashboard.brokerSectionDesc')} extra={<Button type="link" onClick={() => navigate('/nodes')}>{t('app.dashboard.allNodes')} <ArrowRightOutlined /></Button>} />
          <div className="broker-overview-list">
            {brokers.map((broker) => {
              const leaderCount = broker.partitions.filter((item) => item.role === 'LEADER').length;
              const healthyCount = broker.partitions.filter((item) => item.health === 'HEALTHY').length;
              const healthPercent = broker.partitions.length ? Math.round((healthyCount / broker.partitions.length) * 100) : 100;
              return (
                <button className="broker-overview-item" key={broker.memberId} onClick={() => navigate(`/nodes?keyword=${broker.memberId}`)}>
                  <span className="broker-overview-item__icon"><CloudServerOutlined /><i /></span>
                  <span className="broker-overview-item__identity"><strong>{shortNodeName(broker.memberId)}</strong><small>{broker.address}</small></span>
                  <span className="broker-overview-item__roles"><b>{leaderCount}</b> Leaders<small>{broker.partitions.length - leaderCount} Followers</small></span>
                  <span className="broker-overview-item__health"><span>{t('app.common.replicaHealth')} <b>{healthPercent}%</b></span><Progress percent={healthPercent} showInfo={false} strokeColor={healthPercent === 100 ? '#20b486' : '#f0a43b'} size="small" /></span>
                  <ArrowRightOutlined className="broker-overview-item__arrow" />
                </button>
              );
            })}
          </div>
        </div>

        <div className="panel activity-panel">
          <SectionHeader title={t('app.dashboard.activityTitle')} description={t('app.dashboard.activityDesc')} extra={<SyncOutlined className="subtle-spin" />} />
          <div className="activity-list">
            {unhealthy.slice(0, 2).map((item) => (
              <div className="activity-item" key={`${item.group}-${item.id}-${item.broker.memberId}`}>
                <span className="activity-item__dot warning"><WarningFilled /></span>
                <div><strong>{t('app.dashboard.replicaIssue')}</strong><p>{item.broker.memberId} · {item.group}-{item.id}</p><small>{t('app.header.seeMatrix')}</small></div>
              </div>
            ))}
            {sessions.slice(0, 3).map((session) => (
              <div className="activity-item" key={`${session.gateway.memberId}-${session.sessionId}`}>
                <span className="activity-item__dot blue"><ApiOutlined /></span>
                <div><strong>{t('app.dashboard.workerSubscribed')}</strong><p>{session.jobType} · session {session.sessionId}</p><small>{shortNodeName(session.gateway.memberId)}</small></div>
              </div>
            ))}
            <div className="activity-item">
              <span className="activity-item__dot success"><CheckCircleFilled /></span>
              <div><strong>{t('app.dashboard.snapshotSynced')}</strong><p>{t('app.dashboard.snapshotSyncedDesc', { nodes: brokers.length, replicas: partitions.length })}</p><small>{t('app.header.justNow')}</small></div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
