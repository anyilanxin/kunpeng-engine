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
