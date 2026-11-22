import { CloudServerOutlined } from '@ant-design/icons';
import { useBrokers } from '../data/ClusterDataContext';
import { shortNodeName } from '../utils/cluster';

/** Broker 节点间心跳链路视觉：节点卡片与在线徽章取自真实集群快照（最多展示前三个节点） */
export default function NodeClusterVisual() {
  const brokers = useBrokers();
  const slots = ['nc-card--one', 'nc-card--two', 'nc-card--three'];
  const cards = brokers.slice(0, 3);
  const unhealthy = brokers.filter((broker) => broker.partitions.some((partition) => partition.health !== 'HEALTHY')).length;

  return (
    <div className="node-cluster-visual" aria-hidden="true">
      <svg viewBox="0 0 460 180">
        <defs>
          <linearGradient id="nc-flow" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0" stopColor="#78c8ff" />
            <stop offset="1" stopColor="#3778f6" />
          </linearGradient>
          {/* Broker 节点间的心跳链路 */}
          <path id="nc-link-a" d="M184 44 C214 38 238 50 262 68" />
          <path id="nc-link-b" d="M206 136 C230 128 246 108 262 94" />
          <path id="nc-link-c" d="M92 74 C80 88 84 104 98 114" />
        </defs>
        <g className="nc-links">
          <use href="#nc-link-a" /><use href="#nc-link-b" /><use href="#nc-link-c" />
        </g>
        <g className="nc-flows">
          <use href="#nc-link-a" /><use href="#nc-link-b" /><use href="#nc-link-c" />
        </g>
        <circle className="nc-particle" r="3"><animateMotion dur="2.2s" repeatCount="indefinite"><mpath href="#nc-link-a" /></animateMotion></circle>
        <circle className="nc-particle nc-particle--violet" r="3"><animateMotion dur="2.4s" begin=".7s" repeatCount="indefinite"><mpath href="#nc-link-b" /></animateMotion></circle>
        <circle className="nc-particle nc-particle--cyan" r="3"><animateMotion dur="2.8s" begin=".3s" repeatCount="indefinite"><mpath href="#nc-link-c" /></animateMotion></circle>
      </svg>
      {cards.length === 0 && (
        <div className="nc-card nc-card--one">
          <span className="nc-card__icon"><CloudServerOutlined /><i /></span>
          <span className="nc-card__meta"><strong>暂无节点</strong><small>等待集群快照</small></span>
        </div>
      )}
      {cards.map((broker, index) => {
        const leaders = broker.partitions.filter((partition) => partition.role === 'LEADER').length;
        const warn = broker.partitions.some((partition) => partition.health !== 'HEALTHY');
        return (
          <div className={`nc-card ${slots[index]}`} key={broker.memberId}>
            <span className="nc-card__icon"><CloudServerOutlined /><i /></span>
            <span className="nc-card__meta"><strong>{shortNodeName(broker.memberId)}</strong><small>{leaders} Leaders · {broker.partitions.length - leaders}F</small></span>
            <span className={`nc-card__status${warn ? ' nc-card__status--warn' : ''}`}>{warn ? '副本异常' : <><i className="live-dot" />在线</>}</span>
          </div>
        );
      })}
      <span className="nc-badge"><i className="live-dot" />{brokers.length} / {brokers.length} 节点在线{unhealthy ? ` · ${unhealthy} 需关注` : ' · 心跳同步中'}</span>
    </div>
  );
}
