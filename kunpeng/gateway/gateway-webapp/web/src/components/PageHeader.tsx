import type { ReactNode } from 'react';

interface PageHeaderProps {
  eyebrow: string;
  title: string;
  description: string;
  actions?: ReactNode;
  visual?: ReactNode;
  compact?: boolean;
}

export default function PageHeader({ eyebrow, title, description, actions, visual, compact }: PageHeaderProps) {
  return (
    <section className={`page-hero ${compact ? 'page-hero--compact' : ''}`}>
      <div className="page-hero__glow" />
      <div className="page-hero__content">
        <div className="page-hero__eyebrow"><span />{eyebrow}</div>
        <h1>{title}</h1>
        <p>{description}</p>
        {actions && <div className="page-hero__actions">{actions}</div>}
      </div>
      {visual && <div className="page-hero__visual">{visual}</div>}
      <div className="page-hero__grid" aria-hidden="true" />
    </section>
  );
}
