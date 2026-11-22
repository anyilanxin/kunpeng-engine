import type { ReactNode } from 'react';

interface SectionHeaderProps {
  title: string;
  description?: string;
  extra?: ReactNode;
  icon?: ReactNode;
}

export default function SectionHeader({ title, description, extra, icon }: SectionHeaderProps) {
  return (
    <div className="section-header">
      <div className="section-header__title">
        {icon && <span className="section-header__icon">{icon}</span>}
        <div><h2>{title}</h2>{description && <p>{description}</p>}</div>
      </div>
      {extra && <div className="section-header__extra">{extra}</div>}
    </div>
  );
}
