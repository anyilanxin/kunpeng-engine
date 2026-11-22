import { CloudServerOutlined, DeploymentUnitOutlined, PartitionOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useBrokers } from '../data/ClusterDataContext';
import { getPartitionCells, getPartitionGroups, shortNodeName } from '../utils/cluster';
import SectionHeader from './SectionHeader';

export default function PartitionGroupOverview() {
  const navigate = useNavigate();
  const brokers = useBrokers();
  const groups = getPartitionGroups(brokers);
  const cells = getPartitionCells(brokers);

  return (
    <div className="panel partition-overview-panel">
      <SectionHeader
        title="分区运行概览"
        description="逻辑分区与副本角色分布"
        icon={<DeploymentUnitOutlined />}
        extra={<span className="live-label"><i className="live-dot" /> 已同步</span>}
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
              <span className="partition-group-card__content"><strong>{group}</strong><small>{partitionCount} 个逻辑分区 · {groupCells.length} 个副本</small><span className="partition-group-card__track"><i style={{ width: `${healthPercent}%` }} /></span></span>
              <span className="partition-group-card__value"><strong>{healthPercent}%</strong><small>健康</small></span>
            </button>
          );
        })}
      </div>
      <div className="leader-distribution">
        <div className="leader-distribution__header"><span>Leader 节点分布</span><small>按当前快照统计</small></div>
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
