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
import { Button, Descriptions, Drawer, Empty, Input, Select, Table, Tabs, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  ApiOutlined,
  CloudServerOutlined,
  CodeOutlined,
  EyeOutlined,
  GatewayOutlined,
  SearchOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { Trans, useTranslation } from 'react-i18next';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import WorkerPulseVisual from '../components/WorkerPulseVisual';
import { useJobWorkers } from '../data/ClusterDataContext';
import { useMockRefresh } from '../hooks/useMockRefresh';
import type { Gateway, WorkerSession } from '../types/cluster';
import { formatJobType, getWorkerSessions, shortNodeName } from '../utils/cluster';

type SessionRow = WorkerSession & { jobType: string; gateway: Gateway };

const jobColors = ['blue', 'cyan', 'violet', 'orange', 'green'];
const jobColor = (jobType: string) => jobColors[jobType.length % jobColors.length];

/** 订阅会话按 worker 分组：同一 worker 多会话聚合（×N 会话数），不逐会话平铺 */
function groupSessionsByWorker(sessions: { gateway: string; address: string; sessionId: number; worker: string }[]) {
  const groups = new Map<string, { gateway: string; address: string; sessionId: number; worker: string }[]>();
  for (const session of sessions) {
    groups.set(session.worker, [...(groups.get(session.worker) ?? []), session]);
  }
  return Array.from(groups.entries()).map(([worker, list]) => ({ worker, sessions: list }));
}

export default function JobWorkersPage() {
  const { t } = useTranslation();
  const [view, setView] = useState('sessions');
  const [keyword, setKeyword] = useState('');
  const [gatewayId, setGatewayId] = useState('ALL');
  const [selected, setSelected] = useState<SessionRow | null>(null);
  const [selectedSubscription, setSelectedSubscription] = useState<{ jobType: string; sessions: { gateway: string; address: string; sessionId: number; worker: string }[] } | null>(null);
  /** 整页撑满视口：量出页头以下可用高度，页面自身不滚，滚动只发生在表格体内部 */
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
      const footerSpace = footer && footerStyle ? footer.offsetHeight + parseFloat(footerStyle.marginTop) + parseFloat(footerStyle.marginBottom) : 0;
      setFillHeight(Math.max(420, window.innerHeight - page.getBoundingClientRect().top - contentPadding - footerSpace));
    };
    measure();
    window.addEventListener('resize', measure);
    return () => window.removeEventListener('resize', measure);
  }, []);
  const { refreshing } = useMockRefresh();
  const jobWorkerMocks = useJobWorkers();
  const allSessions = getWorkerSessions(jobWorkerMocks);
  const allSubscriptions = jobWorkerMocks.flatMap((snapshot) => snapshot.subscriptions);
  const jobTypes = Array.from(new Set(allSessions.map((session) => session.jobType)));
  const totalCapacity = allSessions.reduce((sum, session) => sum + session.capacity, 0);

  const sessions = useMemo(() => allSessions.filter((session) => {
    const matchesGateway = gatewayId === 'ALL' || session.gateway.memberId === gatewayId;
    const haystack = `${session.jobType} ${session.worker} ${session.sessionId} ${session.tenantIds.join(' ')} ${session.fetchVariables.join(' ')}`.toLowerCase();
    return matchesGateway && haystack.includes(keyword.toLowerCase());
  }), [allSessions, gatewayId, keyword]);

  const subscriptions = useMemo(() => allSubscriptions.map((subscription) => ({
    ...subscription,
    sessions: subscription.sessions.filter((session) => gatewayId === 'ALL' || session.gateway === gatewayId),
  })).filter((subscription) => {
    const matchesKeyword = `${subscription.jobType} ${subscription.sessions.map((session) => `${session.worker} ${session.gateway}`).join(' ')}`.toLowerCase().includes(keyword.toLowerCase());
    return subscription.sessions.length > 0 && matchesKeyword;
  }), [allSubscriptions, gatewayId, keyword]);

  const columns: ColumnsType<SessionRow> = [
    {
      title: t('app.common.jobType'),
      dataIndex: 'jobType',
      width: 190,
      render: (jobType: string) => <div className={`job-type job-type--${jobColor(jobType)}`}><span><ThunderboltOutlined /></span><div><strong>{jobType}</strong><small>{formatJobType(jobType)}</small></div></div>,
    },
    { title: t('app.workers.colWorker'), dataIndex: 'worker', width: 180, render: (worker: string) => <span className="worker-name"><ApiOutlined />{worker}</span> },
    { title: 'Session ID', dataIndex: 'sessionId', width: 110, sorter: (a, b) => a.sessionId - b.sessionId, render: (id: number) => <Tag className="id-tag">#{id}</Tag> },
    {
      title: t('app.workers.colGateway'),
      key: 'gateway',
      width: 180,
      render: (_, session) => <div className="gateway-cell"><strong>{shortNodeName(session.gateway.memberId)}</strong><small>{session.gateway.address}</small></div>,
    },
    { title: 'Capacity', dataIndex: 'capacity', width: 110, sorter: (a, b) => a.capacity - b.capacity, render: (capacity: number) => <span className={`capacity-value ${capacity > 0 ? 'capacity-value--active' : ''}`}><i />{capacity}</span> },
    { title: t('app.workers.colTenant'), dataIndex: 'tenantIds', width: 150, render: (tenants: string[]) => <div className="tag-list">{tenants.map((tenant) => <Tag key={tenant}>{tenant}</Tag>)}</div> },
    {
      title: t('app.workers.colVariables'),
      dataIndex: 'fetchVariables',
      width: 210,
      render: (variables: string[]) => variables.length ? <div className="tag-list">{variables.slice(0, 2).map((variable) => <Tag className="variable-tag" key={variable}><CodeOutlined />{variable}</Tag>)}</div> : <span className="muted-text">—</span>,
    },
    { title: t('app.common.action'), key: 'action', fixed: 'right', width: 90, render: (_, session) => <Button type="link" icon={<EyeOutlined />} onClick={() => setSelected(session)}>{t('app.common.detail')}</Button> },
  ];

  return (
    <div className="page page--fill" ref={pageRef} style={fillHeight ? { height: fillHeight } : undefined}>
      <PageHeader
        compact
        eyebrow="JOBWORKER ORCHESTRATION"
        title={t('app.workers.heroTitle')}
        description={t('app.workers.heroDesc')}
        visual={<WorkerPulseVisual />}
      />

      <section className="metrics-grid">
        <MetricCard title={t('app.workers.metricGatewaysOnline')} value={jobWorkerMocks.length} icon={<GatewayOutlined />} tone="blue" footer={t('app.workers.metricGatewaysFooter')} />
        <MetricCard title={t('app.common.metricSessions')} value={allSessions.length} icon={<ApiOutlined />} tone="cyan" footer={t('app.workers.metricSessionsFooter')} />
        <MetricCard title={t('app.common.metricJobTypes')} value={jobTypes.length} icon={<ThunderboltOutlined />} tone="violet" footer={t('app.common.metricJobTypesFooter')} />
        <MetricCard title={t('app.workers.metricCapacity')} value={totalCapacity} icon={<CloudServerOutlined />} tone="green" footer={t('app.workers.metricCapacityFooter')} />
      </section>

      <section className="panel worker-panel">
        <div className="worker-panel__top">
          <Tabs
            activeKey={view}
            onChange={setView}
            items={[
              { key: 'sessions', label: <span className="worker-tab-label"><ApiOutlined />{t('app.workers.tabSessions')} <b>{allSessions.length}</b></span> },
              { key: 'subscriptions', label: <span className="worker-tab-label"><GatewayOutlined />{t('app.workers.tabSubscriptions')} <b>{allSubscriptions.length}</b></span> },
            ]}
          />
          <div className="table-filters worker-filters">
            <Input allowClear prefix={<SearchOutlined />} placeholder={t('app.workers.searchPlaceholder')} value={keyword} onChange={(event) => setKeyword(event.target.value)} />
            <Select value={gatewayId} onChange={setGatewayId} options={[
              { value: 'ALL', label: t('app.workers.allGateways') },
              ...jobWorkerMocks.map(({ gateway }) => ({ value: gateway.memberId, label: shortNodeName(gateway.memberId) })),
            ]} />
          </div>
        </div>

        {view === 'sessions' ? (
          <div className="worker-table-region">
            <div className="worker-table-summary"><span>{t('app.workers.sessionsSummary')}</span><small>{t('app.workers.sessionsSummaryDesc')}</small><span className="snapshot-label"><i className="live-dot" /> {t('app.workers.liveSnapshot')}</span></div>
            <Table
              rowKey={(record) => `${record.gateway.memberId}-${record.sessionId}`}
              columns={columns}
              dataSource={sessions}
              loading={refreshing}
              pagination={sessions.length > 50 ? { pageSize: 50, showSizeChanger: true, pageSizeOptions: [20, 50, 100], showTotal: (total) => t('app.workers.sessionsTotal', { count: total }) } : false}
              scroll={{ x: 1370, y: 1 }}
              locale={{ emptyText: <Empty description={t('app.workers.emptySessions')} /> }}
            />
            <div className="table-panel__footer"><span>{t('app.workers.sessionsTotal', { count: sessions.length })}</span><small>{t('app.workers.sessionsCapacityNote')}</small></div>
          </div>
        ) : (
          <div className={`worker-table-region subscription-compact ${refreshing ? 'is-refreshing' : ''}`}>
            <div className="worker-table-summary"><span>{t('app.workers.subsSummary')}</span><small>{t('app.workers.subsSummaryDesc', { count: subscriptions.length })}</small><span className="snapshot-label"><i className="live-dot" /> {t('app.workers.liveSnapshot')}</span></div>
            <Table
              rowKey={(record) => record.jobType}
              size="middle"
              dataSource={subscriptions}
              loading={refreshing}
              pagination={subscriptions.length > 50 ? { pageSize: 50, showTotal: (total) => t('app.workers.subsTotal', { count: total }) } : false}
              scroll={{ y: 1 }}
              locale={{ emptyText: <Empty description={t('app.workers.emptySubs')} /> }}
              columns={[
                {
                  title: t('app.common.jobType'),
                  dataIndex: 'jobType',
                  width: 220,
                  render: (jobType: string) => <div className={`job-type job-type--${jobColor(jobType)}`}><span><ThunderboltOutlined /></span><div><strong>{jobType}</strong><small>{formatJobType(jobType)}</small></div></div>,
                },
                { title: t('app.workers.colSubs'), dataIndex: 'sessions', width: 100, sorter: (a, b) => a.sessions.length - b.sessions.length, render: (sessions: unknown[]) => <span className="number-cell number-cell--leader">{sessions.length}</span> },
                {
                  title: t('app.workers.colWorker'),
                  key: 'workers',
                  render: (_, record) => {
                    const groups = groupSessionsByWorker(record.sessions);
                    return <div className="tag-list">{groups.slice(0, 4).map(({ worker, sessions }) => <Tag key={worker}>{worker}{sessions.length > 1 && ` ×${sessions.length}`}</Tag>)}{groups.length > 4 && <Tag>+{groups.length - 4}</Tag>}</div>;
                  },
                },
                {
                  title: t('app.workers.colGateway'),
                  key: 'gateways',
                  width: 200,
                  render: (_, record) => {
                    const gateways = Array.from(new Set(record.sessions.map((session) => session.gateway)));
                    return <div className="tag-list">{gateways.map((gateway) => <Tag key={gateway}>{shortNodeName(gateway)}</Tag>)}</div>;
                  },
                },
                { title: t('app.common.action'), key: 'action', width: 90, render: (_, record) => <Button type="link" icon={<EyeOutlined />} onClick={() => setSelectedSubscription(record)}>{t('app.common.detail')}</Button> },
              ]}
            />
          </div>
        )}
      </section>

      <Drawer
        className="detail-drawer worker-detail-drawer"
        title={t('app.workers.drawerTitle')}
        width={560}
        open={Boolean(selected)}
        onClose={() => setSelected(null)}
        extra={<span className="health-badge health-badge--healthy"><i className="live-dot" />{t('app.workers.subscribing')}</span>}
      >
        {selected && (
          <>
            <div className="worker-detail-hero">
              <span className={`job-type__detail job-type__detail--${jobColor(selected.jobType)}`}><ThunderboltOutlined /></span>
              <div><small>JOB TYPE</small><h3>{selected.jobType}</h3><p>{formatJobType(selected.jobType)}</p></div>
              <Tag className="id-tag">Session #{selected.sessionId}</Tag>
            </div>
            <div className="worker-connection-detail">
              <div><GatewayOutlined /><span><small>Gateway</small><strong>{shortNodeName(selected.gateway.memberId)}</strong></span></div>
              <span className="worker-connection-detail__stream"><i /></span>
              <div><ApiOutlined /><span><small>JobWorker</small><strong>{selected.worker}</strong></span></div>
            </div>
            <Descriptions column={1} bordered size="middle" items={[
              { key: 'worker', label: t('app.workers.labelWorkerName'), children: selected.worker },
              { key: 'address', label: t('app.workers.labelGatewayAddress'), children: selected.gateway.address },
              { key: 'capacity', label: t('app.workers.metricCapacity'), children: <strong>{selected.capacity}</strong> },
              { key: 'tenant', label: t('app.workers.labelTenantList'), children: <div className="tag-list">{selected.tenantIds.map((tenant) => <Tag key={tenant}>{tenant}</Tag>)}</div> },
              { key: 'variables', label: t('app.workers.colVariables'), children: selected.fetchVariables.length ? <div className="tag-list">{selected.fetchVariables.map((variable) => <Tag className="variable-tag" key={variable}><CodeOutlined />{variable}</Tag>)}</div> : t('app.workers.noVariables') },
            ]} />
            <div className="worker-detail-note"><span className="live-dot" /><div><strong>{t('app.workers.noteTitle')}</strong><p><Trans i18nKey="app.workers.noteBody" values={{ jobType: selected.jobType }} components={{ b: <b /> }} /></p></div></div>
          </>
        )}
      </Drawer>

      <Drawer
        className="detail-drawer subscription-detail-drawer"
        title={selectedSubscription ? t('app.workers.subsDrawerTitle', { jobType: selectedSubscription.jobType }) : ''}
        width={560}
        open={Boolean(selectedSubscription)}
        onClose={() => setSelectedSubscription(null)}
        extra={selectedSubscription && <Tag color="blue">{t('app.workers.subsCountTag', { count: selectedSubscription.sessions.length })}</Tag>}
      >
        {selectedSubscription && (
          <>
            <div className="worker-detail-hero">
              <span className={`job-type__detail job-type__detail--${jobColor(selectedSubscription.jobType)}`}><ThunderboltOutlined /></span>
              <div><small>JOB TYPE</small><h3>{selectedSubscription.jobType}</h3><p>{formatJobType(selectedSubscription.jobType)}</p></div>
            </div>
            <div className="subscription-sessions-scroll">
              {groupSessionsByWorker(selectedSubscription.sessions).map(({ worker, sessions }) => (
                <div className="subscription-worker-group" key={worker}>
                  <div className="drawer-section-title"><strong><ApiOutlined /> {worker}</strong><span>{t('app.workers.sessionCount', { count: sessions.length })}</span></div>
                  {sessions.map((session) => (
                    <button
                      className="subscription-session subscription-session--clickable"
                      key={`${session.gateway}-${session.sessionId}`}
                      onClick={() => {
                        const row = allSessions.find((item) => item.jobType === selectedSubscription.jobType && item.sessionId === session.sessionId);
                        if (row) setSelected(row);
                      }}
                    >
                      <div className="subscription-session__identity"><small>Session #{session.sessionId}</small></div>
                      <div className="subscription-session__link"><span /><i /></div>
                      <div className="subscription-session__gateway"><strong><GatewayOutlined />{shortNodeName(session.gateway)}</strong><small>{session.address}</small></div>
                      <span className="subscription-session__status"><i className="live-dot" />ACTIVE</span>
                    </button>
                  ))}
                </div>
              ))}
            </div>
            <p className="dispatch-note">{t('app.workers.clickHint')}</p>
          </>
        )}
      </Drawer>
    </div>
  );
}
