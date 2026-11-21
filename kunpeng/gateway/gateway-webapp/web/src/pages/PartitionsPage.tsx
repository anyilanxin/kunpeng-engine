import PartitionMatrixPage from '../components/PartitionMatrixPage';
import PartitionVisual from '../components/PartitionVisual';

/** 业务分区管理：多 RAFT 组分区（raft-partition） */
export default function PartitionsPage() {
  return (
    <PartitionMatrixPage
      group="raft-partition"
      eyebrow="BUSINESS RAFT PARTITIONS"
      title="业务分区归属，一屏尽览"
      description="多 RAFT 组业务分区，按节点追踪角色、任期与副本健康。"
      visual={<PartitionVisual />}
      refreshText="业务分区快照已刷新"
    />
  );
}
