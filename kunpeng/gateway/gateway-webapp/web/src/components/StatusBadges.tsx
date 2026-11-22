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
