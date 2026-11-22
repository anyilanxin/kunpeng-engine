import PartitionMatrixPage from '../components/PartitionMatrixPage';
import PartitionVisual from '../components/PartitionVisual';

/** 管理分区管理：单 RAFT 组分区（admin-partition），3 副本复制 */
export default function AdminPartitionsPage() {
  return (
    <PartitionMatrixPage
      group="admin-partition"
      eyebrow="ADMIN PARTITIONS"
      title="管理分区，掌控控制面"
      description="单 RAFT 组管理分区，承载集群元数据与控制面指令。"
      visual={(
        <PartitionVisual
          single
          columns={1}
          leaderRows={[0]}
          badge="单 RAFT 组 · 3 副本复制"
        />
      )}
      refreshText="管理分区快照已刷新"
    />
  );
}
