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
