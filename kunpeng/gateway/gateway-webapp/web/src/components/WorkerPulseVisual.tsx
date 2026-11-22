import { ApiOutlined } from '@ant-design/icons';
import { useJobWorkers } from '../data/ClusterDataContext';
import { getWorkerSessions } from '../utils/cluster';

/** Worker 心跳视觉：任务类型胶囊与会话数取自真实快照 */
export default function WorkerPulseVisual() {
  const workers = useJobWorkers();
  const sessions = getWorkerSessions(workers);
  const jobTypes = Array.from(new Set(sessions.map((session) => session.jobType))).slice(0, 3);
  const slots = ['worker-pulse-visual__chip--one', 'worker-pulse-visual__chip--two', 'worker-pulse-visual__chip--three'];

  return (
    <div className="worker-pulse-visual" aria-hidden="true">
      <span className="worker-pulse-visual__line worker-pulse-visual__line--one" />
      <span className="worker-pulse-visual__line worker-pulse-visual__line--two" />
      <span className="worker-pulse-visual__line worker-pulse-visual__line--three" />
      <div className="worker-pulse-visual__core"><ApiOutlined /><small>WORKER</small></div>
      {jobTypes.map((jobType, index) => (
        <span className={`worker-pulse-visual__chip ${slots[index]}`} key={jobType}>{jobType}<i /></span>
      ))}
      <span className="worker-pulse-visual__status"><i className="live-dot" /> {sessions.length ? `${sessions.length} SESSIONS` : '等待订阅'}</span>
    </div>
  );
}
