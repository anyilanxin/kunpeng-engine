export default function LoginClusterVisual() {
  return (
    <div className="login-cluster-visual" aria-hidden="true">
      <div className="login-cluster-visual__orbit login-cluster-visual__orbit--one" />
      <div className="login-cluster-visual__orbit login-cluster-visual__orbit--two" />
      <svg viewBox="0 0 500 330">
        <defs>
          <linearGradient id="login-node" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stopColor="#ffffff" stopOpacity=".96" />
            <stop offset="1" stopColor="#d7e7ff" stopOpacity=".85" />
          </linearGradient>
          <filter id="login-shadow" x="-70%" y="-70%" width="240%" height="240%">
            <feDropShadow dx="0" dy="12" stdDeviation="13" floodColor="#062d83" floodOpacity=".22" />
          </filter>
          {/* 数据流向：JobWorker -> Gateway -> Broker */}
          <path id="login-link-a" d="M104 158 C155 154 162 102 216 100" />
          <path id="login-link-b" d="M104 178 C158 180 164 232 216 234" />
          <path id="login-link-c" d="M284 100 C338 100 342 100 394 100" />
          <path id="login-link-d" d="M286 108 C336 110 348 196 392 206" />
          <path id="login-link-e" d="M286 226 C336 224 348 138 392 128" />
          <path id="login-link-f" d="M284 234 C338 234 342 234 394 234" />
        </defs>
        <g className="login-cluster-links">
          <use href="#login-link-a" /><use href="#login-link-b" />
          <use href="#login-link-c" /><use href="#login-link-d" />
          <use href="#login-link-e" /><use href="#login-link-f" />
        </g>
        <g className="login-cluster-flow">
          <use href="#login-link-a" /><use href="#login-link-b" />
          <use href="#login-link-c" /><use href="#login-link-d" />
          <use href="#login-link-e" /><use href="#login-link-f" />
        </g>
        <g className="login-cluster-node login-cluster-node--worker" transform="translate(72 168)">
          <circle r="44" fill="url(#login-node)" filter="url(#login-shadow)" />
          <circle className="login-cluster-node__inner" r="30" />
          <path d="M-12 -9 H-4 V-3 H4 V-9 H12 V0 H6 V7 H12 V14 H4 V8 H-4 V14 H-12 V7 H-6 V0 H-12 Z" />
          <text y="66">JOBWORKER</text>
        </g>
        <g className="login-cluster-node login-cluster-node--gateway" transform="translate(250 100)">
          <circle className="login-node-pulse login-node-pulse--delay" r="55" />
          <circle r="44" fill="url(#login-node)" filter="url(#login-shadow)" />
          <circle className="login-cluster-node__inner" r="30" />
          <path d="M-12 -10 H12 V10 H-12 Z M-7 -4 H7 M-7 2 H7 M-7 8 H2" />
          <text y="66">GATEWAY 01</text>
        </g>
        <g className="login-cluster-node login-cluster-node--gateway" transform="translate(250 236)">
          <circle r="44" fill="url(#login-node)" filter="url(#login-shadow)" />
          <circle className="login-cluster-node__inner" r="30" />
          <path d="M-12 -10 H12 V10 H-12 Z M-7 -4 H7 M-7 2 H7 M-7 8 H2" />
          <text y="66">GATEWAY 02</text>
        </g>
        <g className="login-cluster-node login-cluster-node--broker" transform="translate(428 100)">
          <circle className="login-node-pulse" r="55" />
          <circle r="44" fill="url(#login-node)" filter="url(#login-shadow)" />
          <circle className="login-cluster-node__inner" r="30" />
          <path d="M-13 -8 L0 -16 L13 -8 V8 L0 16 L-13 8 Z M-13 -8 L0 0 L13 -8 M0 0 V16" />
          <text y="66">BROKER 01</text>
        </g>
        <g className="login-cluster-node login-cluster-node--broker" transform="translate(428 236)">
          <circle className="login-node-pulse login-node-pulse--delay" r="55" />
          <circle r="44" fill="url(#login-node)" filter="url(#login-shadow)" />
          <circle className="login-cluster-node__inner" r="30" />
          <path d="M-13 -8 L0 -16 L13 -8 V8 L0 16 L-13 8 Z M-13 -8 L0 0 L13 -8 M0 0 V16" />
          <text y="66">BROKER 02</text>
        </g>
        <circle className="login-flow-particle" r="4"><animateMotion dur="2.8s" repeatCount="indefinite"><mpath href="#login-link-a" /></animateMotion></circle>
        <circle className="login-flow-particle" r="4"><animateMotion dur="2.8s" begin="1.2s" repeatCount="indefinite"><mpath href="#login-link-b" /></animateMotion></circle>
        <circle className="login-flow-particle" r="4"><animateMotion dur="2.6s" begin=".5s" repeatCount="indefinite"><mpath href="#login-link-c" /></animateMotion></circle>
        <circle className="login-flow-particle" r="4"><animateMotion dur="2.6s" begin="1.7s" repeatCount="indefinite"><mpath href="#login-link-f" /></animateMotion></circle>
      </svg>
      <div className="login-cluster-visual__badge login-cluster-visual__badge--leader"><i /> BROKER <strong>分区承载</strong></div>
      <div className="login-cluster-visual__badge login-cluster-visual__badge--health"><i /> GATEWAY <strong>集群接入</strong></div>
    </div>
  );
}
