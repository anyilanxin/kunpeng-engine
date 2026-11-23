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
import PartitionMatrixPage from '../components/PartitionMatrixPage';
import PartitionVisual from '../components/PartitionVisual';

/** 业务分区管理：多 RAFT 组分区（raft-partition） */
export default function PartitionsPage() {
  return (
    <PartitionMatrixPage
      group="raft-partition"
      eyebrow="BUSINESS RAFT PARTITIONS"
      titleKey="app.partitions.businessTitle"
      descriptionKey="app.partitions.businessDesc"
      visual={<PartitionVisual />}
    />
  );
}
