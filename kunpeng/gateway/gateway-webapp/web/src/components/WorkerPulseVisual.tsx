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
import { ApiOutlined } from '@ant-design/icons';
import { useJobWorkers } from '../data/ClusterDataContext';
import { getWorkerSessions } from '../utils/cluster';
import { useTranslation } from 'react-i18next';

/** Worker 心跳视觉：任务类型胶囊与会话数取自真实快照 */
export default function WorkerPulseVisual() {
  const { t } = useTranslation();
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
      <span className="worker-pulse-visual__status"><i className="live-dot" />{sessions.length ? t('app.visuals.workerOnlineBadge', { count: sessions.length }) : t('app.visuals.workerWaiting')}</span>
    </div>
  );
}
