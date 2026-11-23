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
import { CheckCircleFilled, ExclamationCircleFilled, QuestionCircleFilled } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import type { PartitionHealth, PartitionRole } from '../types/cluster';

export function RoleBadge({ role }: { role: PartitionRole }) {
  return <span className={`role-badge role-badge--${role.toLowerCase()}`}>{role.charAt(0) + role.slice(1).toLowerCase()}</span>;
}

export function HealthBadge({ health }: { health: PartitionHealth }) {
  const { t } = useTranslation();
  const healthy = health === 'HEALTHY';
  return (
    <span className={`health-badge health-badge--${health.toLowerCase()}`}>
      {health === 'UNKNOWN' ? <QuestionCircleFilled /> : healthy ? <CheckCircleFilled /> : <ExclamationCircleFilled />}
      {healthy ? t('app.common.healthy') : health === 'UNHEALTHY' ? t('app.common.unhealthy') : t('app.common.unknown')}
    </span>
  );
}
