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
