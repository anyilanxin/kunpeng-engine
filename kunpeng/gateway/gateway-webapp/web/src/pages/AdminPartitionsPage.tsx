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
import { useTranslation } from 'react-i18next';
import PartitionMatrixPage from '../components/PartitionMatrixPage';
import PartitionVisual from '../components/PartitionVisual';

/** 管理分区管理：单 RAFT 组分区（admin-partition），3 副本复制 */
export default function AdminPartitionsPage() {
  const { t } = useTranslation();
  return (
    <PartitionMatrixPage
      group="admin-partition"
      eyebrow="ADMIN PARTITIONS"
      titleKey="app.partitions.adminTitle"
      descriptionKey="app.partitions.adminDesc"
      visual={(
        <PartitionVisual
          single
          columns={1}
          leaderRows={[0]}
          badge={t('app.partitions.adminBadge')}
        />
      )}
    />
  );
}
