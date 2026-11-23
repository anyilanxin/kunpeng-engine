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
import type { ReactNode } from 'react';

interface PageHeaderProps {
  eyebrow: string;
  title: string;
  description: string;
  actions?: ReactNode;
  visual?: ReactNode;
  compact?: boolean;
  /** hero 内容流内的工具条插槽（渲染在描述之下，如集群大屏的节点切换器） */
  toolbar?: ReactNode;
}

export default function PageHeader({ eyebrow, title, description, actions, visual, compact, toolbar }: PageHeaderProps) {
  return (
    <section className={`page-hero ${compact ? 'page-hero--compact' : ''}`}>
      <div className="page-hero__glow" />
      <div className="page-hero__content">
        <div className="page-hero__eyebrow"><span />{eyebrow}</div>
        <h1>{title}</h1>
        <p>{description}</p>
        {toolbar && <div className="page-hero__toolbar">{toolbar}</div>}
        {actions && <div className="page-hero__actions">{actions}</div>}
      </div>
      {visual && <div className="page-hero__visual">{visual}</div>}
      <div className="page-hero__grid" aria-hidden="true" />
    </section>
  );
}
