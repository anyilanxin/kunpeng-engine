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
import { CloudServerOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

/** Broker 节点间心跳链路视觉：概念示意死数据（装饰视觉两分法），固定三节点、不接真实集群状态 */
export default function NodeClusterVisual() {
  const { t } = useTranslation();
  const nodes = [
    { slot: 'nc-card--one', name: 'node-01', meta: '4 Leaders · 1F' },
    { slot: 'nc-card--two', name: 'node-02', meta: '3 Leaders · 2F' },
    { slot: 'nc-card--three', name: 'node-03', meta: '4 Leaders · 1F' },
  ];

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
      {nodes.map((node) => (
        <div className={`nc-card ${node.slot}`} key={node.name}>
          <span className="nc-card__icon"><CloudServerOutlined /><i /></span>
          <span className="nc-card__meta"><strong>{node.name}</strong><small>{node.meta}</small></span>
          <span className="nc-card__status"><i className="live-dot" />{t('app.visuals.online')}</span>
        </div>
      ))}
      <span className="nc-badge"><i className="live-dot" />{t('app.visuals.nodeOnlineBadge', { total: nodes.length })}{t('app.visuals.nodeHeartbeat')}</span>
    </div>
  );
}
