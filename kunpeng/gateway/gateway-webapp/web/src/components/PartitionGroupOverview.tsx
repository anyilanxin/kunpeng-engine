import { CloudServerOutlined, DeploymentUnitOutlined, PartitionOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useBrokers } from '../data/ClusterDataContext';
import { getPartitionCells, getPartitionGroups, shortNodeName } from '../utils/cluster';
import SectionHeader from './SectionHeader';

export default function PartitionGroupOverview() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const brokers = useBrokers();
  const groups = getPartitionGroups(brokers);
  const cells = getPartitionCells(brokers);

  return (
    <div className="panel partition-overview-panel">
      <SectionHeader
        title={t('app.dashboard.groupTitle')}
        description={t('app.dashboard.groupDesc')}
        icon={<DeploymentUnitOutlined />}
        extra={<span className="live-label"><i className="live-dot" /> {t('app.dashboard.synced')}</span>}
      />
      <div className="partition-group-cards">
        {groups.map((group, index) => {
          const groupCells = cells.filter((cell) => cell.group === group);
          const partitionCount = new Set(groupCells.map((cell) => cell.id)).size;
          const healthyCount = groupCells.filter((cell) => cell.health === 'HEALTHY').length;
          const healthPercent = groupCells.length ? Math.round((healthyCount / groupCells.length) * 100) : 100;
          return (
            <button className={`partition-group-card partition-group-card--${index === 0 ? 'blue' : 'violet'}`} key={group} onClick={() => navigate(group === 'admin-partition' ? '/admin-partitions' : '/partitions')}>
              <span className="partition-group-card__icon"><PartitionOutlined /></span>
              <span className="partition-group-card__content"><strong>{group}</strong><small>{t('app.dashboard.groupStats', { partitions: partitionCount, replicas: groupCells.length })}</small><span className="partition-group-card__track"><i style={{ width: `${healthPercent}%` }} /></span></span>
              <span className="partition-group-card__value"><strong>{healthPercent}%</strong><small>{t('app.common.healthy')}</small></span>
            </button>
          );
        })}
      </div>
      <div className="leader-distribution">
        <div className="leader-distribution__header"><span>{t('app.dashboard.leaderDistribution')}</span><small>{t('app.dashboard.bySnapshot')}</small></div>
        <div className="leader-distribution__chart">
          {brokers.map((broker, index) => {
            const count = broker.partitions.filter((partition) => partition.role === 'LEADER').length;
            return (
              <div className="leader-column" key={broker.memberId}>
                <div className="leader-column__bars">
                  {Array.from({ length: count }, (_, barIndex) => <i key={barIndex} style={{ animationDelay: `${index * 120 + barIndex * 70}ms` }} />)}
                </div>
                <strong>{count}</strong>
                <span><CloudServerOutlined />{shortNodeName(broker.memberId)}</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
