import type { ReactNode } from 'react';
import { ArrowDownOutlined, ArrowUpOutlined } from '@ant-design/icons';

interface MetricCardProps {
  title: string;
  value: ReactNode;
  icon: ReactNode;
  tone?: 'blue' | 'cyan' | 'violet' | 'green';
  footer?: ReactNode;
  trend?: number;
}

export default function MetricCard({ title, value, icon, tone = 'blue', footer, trend }: MetricCardProps) {
  return (
    <article className={`metric-card metric-card--${tone}`}>
      <div className="metric-card__top">
        <span className="metric-card__title">{title}</span>
        <span className="metric-card__icon">{icon}</span>
      </div>
      <div className="metric-card__value">{value}</div>
      <div className="metric-card__footer">
        {trend !== undefined && (
          <span className={trend >= 0 ? 'trend trend--up' : 'trend trend--down'}>
            {trend >= 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />}{Math.abs(trend)}%
          </span>
        )}
        <span>{footer}</span>
      </div>
      <span className="metric-card__accent" />
    </article>
  );
}
