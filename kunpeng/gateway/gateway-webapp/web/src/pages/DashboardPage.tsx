import { Button, Progress, Tag } from 'antd';
import {
  ApiOutlined,
  ArrowRightOutlined,
  CheckCircleFilled,
  CloudServerOutlined,
  PartitionOutlined,
  ReloadOutlined,
  SyncOutlined,
  ThunderboltOutlined,
  WarningFilled,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import ClusterTopology from '../components/ClusterTopology';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionHeader from '../components/SectionHeader';
import PartitionGroupOverview from '../components/PartitionGroupOverview';
import { useBrokers, useJobWorkers } from '../data/ClusterDataContext';
import { useMockRefresh } from '../hooks/useMockRefresh';
import { getPartitionCells, getWorkerSessions, shortNodeName } from '../utils/cluster';

export default function DashboardPage() {
  const navigate = useNavigate();
  const { refreshing, refresh } = useMockRefresh();
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
        title="流程无界，集群有序。"
        description="统一感知 Broker、Raft 分区与 JobWorker，让每一个流程任务可靠抵达。"
        visual={<ClusterTopology />}
        actions={
          <>
            <Button type="primary" icon={<PartitionOutlined />} onClick={() => navigate('/partitions')}>查看分区矩阵</Button>
            <Button icon={<ReloadOutlined spin={refreshing} />} onClick={refresh}>刷新数据</Button>
          </>
        }
      />

      <section className="metrics-grid">
        <MetricCard title="在线 Broker" value={<>{brokers.length}<small> / {brokers.length}</small></>} icon={<CloudServerOutlined />} tone="blue" footer="全部节点在线" trend={0} />
        <MetricCard title="逻辑分区" value={new Set(partitions.map((item) => `${item.group}-${item.id}`)).size} icon={<PartitionOutlined />} tone="cyan" footer={`${leaders} 个 Leader · ${followers} 个 Follower`} />
        <MetricCard title="Worker 会话" value={sessions.length} icon={<ApiOutlined />} tone="violet" footer={`${jobTypes.size} 种任务类型已订阅`} />
        <MetricCard title="任务类型" value={jobTypes.size} icon={<ThunderboltOutlined />} tone="green" footer="覆盖流程任务与消息事件" />
      </section>

      <section className="dashboard-primary-grid">
        <PartitionGroupOverview />

        <div className="panel cluster-health-panel">
          <SectionHeader title="集群健康" description="副本健康快照" extra={<Tag color={unhealthy.length ? 'warning' : 'success'} className="soft-tag">{unhealthy.length ? '需要关注' : '整体稳定'}</Tag>} />
          <div className="health-score">
            <div className="health-donut">
              <svg viewBox="0 0 120 120">
                <circle className="health-donut__track" cx="60" cy="60" r="50" pathLength="100" />
                <circle className="health-donut__value" cx="60" cy="60" r="50" pathLength="100" strokeDasharray={`${healthPercent} 100`} />
              </svg>
              <div><strong>{healthPercent}<small>%</small></strong><span>健康评分</span></div>
            </div>
            <div className="health-score__legend">
              <div><span><i className="dot dot--blue" />健康副本</span><strong>{healthy}</strong></div>
              <div><span><i className="dot dot--red" />异常副本</span><strong className="danger-text">{partitions.length - healthy}</strong></div>
              <div><span><i className="dot dot--gray" />当前变更</span><strong>无</strong></div>
            </div>
          </div>
          <div className="health-role-bars">
            <div><span>Leader <b>{leaders}</b></span><Progress percent={partitions.length ? (leaders / partitions.length) * 100 : 0} showInfo={false} strokeColor="#2f6ff4" trailColor="#edf2fa" size="small" /></div>
            <div><span>Follower <b>{followers}</b></span><Progress percent={partitions.length ? (followers / partitions.length) * 100 : 0} showInfo={false} strokeColor="#8fb6f8" trailColor="#edf2fa" size="small" /></div>
          </div>
          {unhealthy.length > 0 && (
            <button className="health-warning" onClick={() => navigate('/partitions')}>
              <WarningFilled /> {unhealthy.slice(0, 2).map((item) => `${item.group}-${item.id}`).join('、')}{unhealthy.length > 2 ? ` 等 ${unhealthy.length} 个副本` : ''} 状态异常 <ArrowRightOutlined />
            </button>
          )}
        </div>
      </section>

      <section className="dashboard-secondary-grid">
        <div className="panel broker-overview-panel">
          <SectionHeader title="Broker 节点" description="节点分区分布与副本健康状态" extra={<Button type="link" onClick={() => navigate('/nodes')}>全部节点 <ArrowRightOutlined /></Button>} />
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
                  <span className="broker-overview-item__health"><span>副本健康 <b>{healthPercent}%</b></span><Progress percent={healthPercent} showInfo={false} strokeColor={healthPercent === 100 ? '#20b486' : '#f0a43b'} size="small" /></span>
                  <ArrowRightOutlined className="broker-overview-item__arrow" />
                </button>
              );
            })}
          </div>
        </div>

        <div className="panel activity-panel">
          <SectionHeader title="集群动态" description="来自当前快照的集群事实" extra={<SyncOutlined className="subtle-spin" />} />
          <div className="activity-list">
            {unhealthy.slice(0, 2).map((item) => (
              <div className="activity-item" key={`${item.group}-${item.id}-${item.broker.memberId}`}>
                <span className="activity-item__dot warning"><WarningFilled /></span>
                <div><strong>副本健康状态异常</strong><p>{item.broker.memberId} · {item.group}-{item.id}</p><small>见分区矩阵</small></div>
              </div>
            ))}
            {sessions.slice(0, 3).map((session) => (
              <div className="activity-item" key={`${session.gateway.memberId}-${session.sessionId}`}>
                <span className="activity-item__dot blue"><ApiOutlined /></span>
                <div><strong>Worker 已建立订阅</strong><p>{session.jobType} · session {session.sessionId}</p><small>{shortNodeName(session.gateway.memberId)}</small></div>
              </div>
            ))}
            <div className="activity-item">
              <span className="activity-item__dot success"><CheckCircleFilled /></span>
              <div><strong>集群快照已同步</strong><p>{brokers.length} 个节点 · {partitions.length} 个分区副本</p><small>刚刚</small></div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
