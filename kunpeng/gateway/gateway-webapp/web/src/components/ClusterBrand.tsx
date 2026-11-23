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
/** 品牌标识：用户提供的 logo.png（public 静态资产），KUNPENG 控制台 */
import { useTranslation } from 'react-i18next';

export default function ClusterBrand({ compact = false }: { compact?: boolean }) {
  const { t } = useTranslation();

  return (
    <div className={`cluster-brand ${compact ? 'cluster-brand--compact' : ''}`}>
      <img className="cluster-brand__mark" src="logo.png" alt="" aria-hidden="true" />
      {!compact && (
        <span className="cluster-brand__text">
          <strong>KUNPENG</strong>
          <small>{t('app.brand.subtitle')}</small>
        </span>
      )}
    </div>
  );
}
