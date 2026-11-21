import { useMemo, useState } from 'react';
import { Button, Descriptions, Drawer, Empty, Input, Select, Table, Tabs, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  ApiOutlined,
  ArrowRightOutlined,
  CloudServerOutlined,
  CodeOutlined,
  EyeOutlined,
  GatewayOutlined,
  ReloadOutlined,
  SearchOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
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

export default function JobWorkersPage() {
  const [view, setView] = useState('sessions');
  const [keyword, setKeyword] = useState('');
  const [gatewayId, setGatewayId] = useState('ALL');
  const [selected, setSelected] = useState<SessionRow | null>(null);
  const { refreshing, refresh } = useMockRefresh('JobWorker 快照已刷新');
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
      title: '任务类型',
      dataIndex: 'jobType',
      width: 190,
      render: (jobType: string) => <div className={`job-type job-type--${jobColor(jobType)}`}><span><ThunderboltOutlined /></span><div><strong>{jobType}</strong><small>{formatJobType(jobType)}</small></div></div>,
    },
    { title: 'Worker', dataIndex: 'worker', width: 180, render: (worker: string) => <span className="worker-name"><ApiOutlined />{worker}</span> },
    { title: 'Session ID', dataIndex: 'sessionId', width: 110, sorter: (a, b) => a.sessionId - b.sessionId, render: (id: number) => <Tag className="id-tag">#{id}</Tag> },
    {
      title: 'Gateway',
      key: 'gateway',
      width: 180,
      render: (_, session) => <div className="gateway-cell"><strong>{shortNodeName(session.gateway.memberId)}</strong><small>{session.gateway.address}</small></div>,
    },
    { title: 'Capacity', dataIndex: 'capacity', width: 110, sorter: (a, b) => a.capacity - b.capacity, render: (capacity: number) => <span className={`capacity-value ${capacity > 0 ? 'capacity-value--active' : ''}`}><i />{capacity}</span> },
    { title: '租户', dataIndex: 'tenantIds', width: 150, render: (tenants: string[]) => <div className="tag-list">{tenants.map((tenant) => <Tag key={tenant}>{tenant}</Tag>)}</div> },
    {
      title: '抓取变量',
      dataIndex: 'fetchVariables',
      width: 210,
      render: (variables: string[]) => variables.length ? <div className="tag-list">{variables.slice(0, 2).map((variable) => <Tag className="variable-tag" key={variable}><CodeOutlined />{variable}</Tag>)}</div> : <span className="muted-text">—</span>,
    },
    { title: '操作', key: 'action', fixed: 'right', width: 90, render: (_, session) => <Button type="link" icon={<EyeOutlined />} onClick={() => setSelected(session)}>详情</Button> },
  ];

  return (
    <div className="page">
      <PageHeader
        compact
        eyebrow="JOBWORKER ORCHESTRATION"
        title="任务有去处，执行有回响"
        description="管理 Gateway 中的 JobWorker 会话、任务订阅与变量抓取配置。"
        visual={<WorkerPulseVisual />}
        actions={<Button icon={<ReloadOutlined spin={refreshing} />} onClick={refresh}>刷新 Worker 状态</Button>}
      />

      <section className="metrics-grid">
        <MetricCard title="在线 Gateway" value={jobWorkerMocks.length} icon={<GatewayOutlined />} tone="blue" footer="均已建立连接" />
        <MetricCard title="Worker 会话" value={allSessions.length} icon={<ApiOutlined />} tone="cyan" footer="全部处于订阅状态" />
        <MetricCard title="任务类型" value={jobTypes.length} icon={<ThunderboltOutlined />} tone="violet" footer="覆盖流程任务与消息事件" />
        <MetricCard title="可用 Capacity" value={totalCapacity} icon={<CloudServerOutlined />} tone="green" footer="所有 Worker 容量之和" />
      </section>

      <section className="panel worker-panel">
        <div className="worker-panel__top">
          <Tabs
            activeKey={view}
            onChange={setView}
            items={[
              { key: 'sessions', label: <span className="worker-tab-label"><ApiOutlined />Worker 会话 <b>{allSessions.length}</b></span> },
              { key: 'subscriptions', label: <span className="worker-tab-label"><GatewayOutlined />订阅关系 <b>{allSubscriptions.length}</b></span> },
            ]}
          />
          <div className="table-filters worker-filters">
            <Input allowClear prefix={<SearchOutlined />} placeholder="搜索任务类型或 Worker" value={keyword} onChange={(event) => setKeyword(event.target.value)} />
            <Select value={gatewayId} onChange={setGatewayId} options={[
              { value: 'ALL', label: '全部 Gateway' },
              ...jobWorkerMocks.map(({ gateway }) => ({ value: gateway.memberId, label: shortNodeName(gateway.memberId) })),
            ]} />
          </div>
        </div>

        {view === 'sessions' ? (
          <>
            <div className="worker-table-summary"><span>会话连接</span><small>展示各 Gateway 已注册的 JobWorker 与订阅配置</small><span className="snapshot-label"><i className="live-dot" /> 实时快照</span></div>
            <Table
              rowKey={(record) => `${record.gateway.memberId}-${record.sessionId}`}
              columns={columns}
              dataSource={sessions}
              loading={refreshing}
              pagination={false}
              scroll={{ x: 1370 }}
              locale={{ emptyText: <Empty description="没有符合条件的 Worker 会话" /> }}
            />
            <div className="table-panel__footer"><span>共 {sessions.length} 个会话</span><small>Capacity 为 Worker 当前声明的可用任务容量</small></div>
          </>
        ) : (
          <div className={`subscription-grid ${refreshing ? 'is-refreshing' : ''}`}>
            {subscriptions.map((subscription, index) => (
              <article className="subscription-card" key={subscription.jobType} style={{ animationDelay: `${index * 70}ms` }}>
                <div className="subscription-card__header">
                  <div className={`job-type job-type--${jobColor(subscription.jobType)}`}><span><ThunderboltOutlined /></span><div><strong>{subscription.jobType}</strong><small>{formatJobType(subscription.jobType)}</small></div></div>
                  <Tag color="blue">{subscription.sessions.length} 个订阅</Tag>
                </div>
                <div className="subscription-card__body">
                  {subscription.sessions.map((session) => (
                    <div className="subscription-session" key={`${session.gateway}-${session.sessionId}`}>
                      <span className="subscription-session__worker"><ApiOutlined /></span>
                      <div className="subscription-session__identity"><strong>{session.worker}</strong><small>Session #{session.sessionId}</small></div>
                      <div className="subscription-session__link"><span /><i /></div>
                      <div className="subscription-session__gateway"><strong><GatewayOutlined />{shortNodeName(session.gateway)}</strong><small>{session.address}</small></div>
                      <span className="subscription-session__status"><i className="live-dot" />ACTIVE</span>
                    </div>
                  ))}
                </div>
                <div className="subscription-card__footer"><span><i className="live-dot" /> 订阅通道正常</span><button onClick={() => {
                  const session = allSessions.find((item) => item.jobType === subscription.jobType && item.sessionId === subscription.sessions[0].sessionId);
                  if (session) setSelected(session);
                }}>查看会话 <ArrowRightOutlined /></button></div>
              </article>
            ))}
            {subscriptions.length === 0 && <Empty description="没有符合条件的订阅关系" />}
          </div>
        )}
      </section>

      <Drawer
        className="detail-drawer worker-detail-drawer"
        title="JobWorker 会话详情"
        width={560}
        open={Boolean(selected)}
        onClose={() => setSelected(null)}
        extra={<span className="health-badge health-badge--healthy"><i className="live-dot" />订阅中</span>}
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
              { key: 'worker', label: 'Worker 名称', children: selected.worker },
              { key: 'address', label: 'Gateway 地址', children: selected.gateway.address },
              { key: 'capacity', label: '可用 Capacity', children: <strong>{selected.capacity}</strong> },
              { key: 'tenant', label: '租户列表', children: <div className="tag-list">{selected.tenantIds.map((tenant) => <Tag key={tenant}>{tenant}</Tag>)}</div> },
              { key: 'variables', label: '抓取变量', children: selected.fetchVariables.length ? <div className="tag-list">{selected.fetchVariables.map((variable) => <Tag className="variable-tag" key={variable}><CodeOutlined />{variable}</Tag>)}</div> : '不抓取变量' },
            ]} />
            <div className="worker-detail-note"><span className="live-dot" /><div><strong>长连接订阅正常</strong><p>Gateway 会将匹配 <b>{selected.jobType}</b> 类型的流程任务推送到此 Worker。</p></div></div>
          </>
        )}
      </Drawer>
    </div>
  );
}
