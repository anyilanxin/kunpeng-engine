import { useTranslation } from 'react-i18next';

export default function ClusterTopology() {
  const { t } = useTranslation();
  return (
    <div className="cluster-topology" aria-label={t('app.visuals.topologyAria')}>
      <div className="cluster-topology__caption"><span className="live-dot" /> LIVE TOPOLOGY <span>{t('app.visuals.topologyCaption')}</span></div>
      <svg viewBox="0 0 560 220" role="img" aria-hidden="true">
        <defs>
          <linearGradient id="flow-line" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0" stopColor="#78c8ff" />
            <stop offset="1" stopColor="#3778f6" />
          </linearGradient>
          <linearGradient id="node-fill" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="#ffffff" />
            <stop offset="1" stopColor="#edf4ff" />
          </linearGradient>
          <filter id="node-shadow" x="-50%" y="-50%" width="200%" height="200%">
            <feDropShadow dx="0" dy="7" stdDeviation="8" floodColor="#3979da" floodOpacity=".14" />
          </filter>
          {/* 数据流向：外部客户端（流程客户端 / JobWorker）-> Gateway -> Broker（承载 Raft 分区） */}
          <path id="route-client" d="M94 74 C150 70 190 96 226 102" />
          <path id="route-worker" d="M94 150 C150 154 190 126 226 120" />
          <path id="route-broker-a" d="M300 100 C350 92 390 72 429 68" />
          <path id="route-broker-b" d="M300 122 C350 130 390 150 429 154" />
        </defs>

        <g className="topology-links">
          <use href="#route-client" />
          <use href="#route-worker" />
          <use href="#route-broker-a" />
          <use href="#route-broker-b" />
        </g>
        <g className="topology-flows">
          <use href="#route-client" />
          <use href="#route-worker" />
          <use href="#route-broker-a" />
          <use href="#route-broker-b" />
        </g>

        {/* 外部客户端：与 JobWorker 对等 */}
        <g className="topology-node topology-node--start" transform="translate(58 64)">
          <circle className="topology-node__outer" r="32" fill="url(#node-fill)" filter="url(#node-shadow)" />
          <circle className="topology-node__icon" r="21" />
          <path d="M-8 -5 L0 -11 L8 -5 L8 5 L0 11 L-8 5 Z M0 -11 V0 M-8 5 L0 0 L8 5" />
          <text y="52">{t('app.visuals.topologyClient')}</text>
        </g>

        <g className="topology-node topology-node--worker" transform="translate(58 158)">
          <circle className="topology-node__outer" r="32" fill="url(#node-fill)" filter="url(#node-shadow)" />
          <circle className="topology-node__icon topology-node__icon--green" r="21" />
          <path d="M-10 -7 H-3 V-2 H3 V-7 H10 V0 H5 V6 H10 V12 H3 V7 H-3 V12 H-10 V6 H-5 V0 H-10 Z" />
          <text y="52">JobWorker</text>
        </g>

        {/* Gateway：外部客户端与 Broker 通信的桥梁 */}
        <g className="topology-node" transform="translate(262 111)">
          <circle className="topology-pulse topology-pulse--delay" r="42" />
          <circle className="topology-node__outer" r="34" fill="url(#node-fill)" filter="url(#node-shadow)" />
          <circle className="topology-node__icon topology-node__icon--cyan" r="23" />
          <path d="M-9 -8 H9 V8 H-9 Z M-5 -3 H5 M-5 2 H5 M-5 7 H1" />
          <text x="-22" y="-47">Gateway</text>
          <text className="topology-node__meta" x="-22" y="53">{t('app.visuals.topologyGateway')}</text>
        </g>

        {/* Broker 节点：Raft 分区直接承载在 Broker 上 */}
        <g className="topology-node topology-node--broker" transform="translate(462 64)">
          <circle className="topology-pulse" r="40" />
          <circle className="topology-node__outer" r="31" fill="url(#node-fill)" filter="url(#node-shadow)" />
          <circle className="topology-node__icon topology-node__icon--violet" r="20" />
          <path d="M-10 -7 L0 -13 L10 -7 V7 L0 13 L-10 7 Z M-10 -7 L0 0 L10 -7 M0 0 V13" />
          <g className="topology-partitions">
            <rect x="-13" y="24" width="8" height="5" rx="1.5" />
            <rect className="topology-partition-chip--leader" x="-4" y="24" width="8" height="5" rx="1.5" />
            <rect x="5" y="24" width="8" height="5" rx="1.5" />
          </g>
          <title>{t('app.visuals.topologyBrokerTitle')}</title>
          <text y="52">Broker 01</text>
        </g>

        <g className="topology-node topology-node--broker" transform="translate(462 158)">
          <circle className="topology-pulse topology-pulse--delay" r="40" />
          <circle className="topology-node__outer" r="31" fill="url(#node-fill)" filter="url(#node-shadow)" />
          <circle className="topology-node__icon topology-node__icon--violet" r="20" />
          <path d="M-10 -7 L0 -13 L10 -7 V7 L0 13 L-10 7 Z M-10 -7 L0 0 L10 -7 M0 0 V13" />
          <g className="topology-partitions">
            <rect x="-13" y="24" width="8" height="5" rx="1.5" />
            <rect x="-4" y="24" width="8" height="5" rx="1.5" />
            <rect x="5" y="24" width="8" height="5" rx="1.5" />
          </g>
          <title>{t('app.visuals.topologyBrokerTitle')}</title>
          <text y="52">Broker 02</text>
        </g>

        <circle className="flow-particle" r="4"><animateMotion dur="2.4s" repeatCount="indefinite"><mpath href="#route-client" /></animateMotion></circle>
        <circle className="flow-particle flow-particle--green" r="4"><animateMotion dur="2.6s" begin=".8s" repeatCount="indefinite"><mpath href="#route-worker" /></animateMotion></circle>
        <circle className="flow-particle flow-particle--cyan" r="4"><animateMotion dur="2.2s" begin=".4s" repeatCount="indefinite"><mpath href="#route-broker-a" /></animateMotion></circle>
        <circle className="flow-particle flow-particle--violet" r="4"><animateMotion dur="2.4s" begin="1.2s" repeatCount="indefinite"><mpath href="#route-broker-b" /></animateMotion></circle>
      </svg>
    </div>
  );
}
