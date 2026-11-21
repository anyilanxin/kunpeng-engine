import { api, time } from '../api';
import { usePolling } from '../usePolling';
import { ErrorAlert } from '../components/ErrorAlert';
import { StateTag } from '../components/StatusBadge';
import PageHeader from '../components/PageHeader';
import SectionHeader from '../components/SectionHeader';
import PartitionVisual from '../components/PartitionVisual';
import type { DispatchExecutionVo, DispatchVo } from '../types';
import { Alert, Button, Descriptions, Drawer, Empty, Table, Tag, Typography } from 'antd';
import { ReloadOutlined, SwapOutlined } from '@ant-design/icons';
import { useState } from 'react';

function ExecutionsTimeline({ executions }: { executions: DispatchExecutionVo[] }) {
  if (!executions.length) {
    return <Typography.Text type="secondary">无执行明细</Typography.Text>;
  }
  return (
    <div className="dispatch-executions">
      {executions.map((e) => (
        <div className={`dispatch-execution dispatch-execution--${e.state.toLowerCase()}`} key={e.dispatchPlanExecutionId}>
          <span className="dispatch-execution__rail"><i /></span>
          <div className="dispatch-execution__body">
            <div className="dispatch-execution__title">
              <Typography.Text code>#{e.executionOrder}</Typography.Text>
              <StateTag state={e.state} />
              {e.planType && <Tag>{e.planType}</Tag>}
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {e.memberId || '-'}
              </Typography.Text>
            </div>
            <div className="dispatch-execution__meta">
              <span>开始 {time(e.startTime)} · 结束 {time(e.endTime)}</span>
              {e.remaining >= 0 && <span>剩余量 {e.remaining}</span>}
              {e.message && <span className="dispatch-execution__message">{e.message}</span>}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

interface MetaRow {
  partitionGroup: string;
  partitionId: number;
  primaryMemberId: string;
  partitionMembers: { memberId: string; priority: number }[];
}

function PartitionMetaTable({ rows }: { rows: MetaRow[] }) {
  return (
    <Table
      rowKey={(r) => `${r.partitionGroup}:${r.partitionId}`}
      size="small"
      dataSource={rows ?? []}
      pagination={false}
      columns={[
        { title: '组', dataIndex: 'partitionGroup' },
        { title: '分区', dataIndex: 'partitionId', render: (v: number) => `#${v}` },
        { title: '主成员', dataIndex: 'primaryMemberId' },
        {
          title: '成员',
          dataIndex: 'partitionMembers',
          render: (ms: MetaRow['partitionMembers']) =>
            ms.map((m) => `${m.memberId}(${m.priority})`).join(' / '),
        },
      ]}
    />
  );
}

export default function DispatchPage() {
  const { data, error, loading, refresh } = usePolling<DispatchVo>(() => api<DispatchVo>('/dispatch'));
  const [metaOpen, setMetaOpen] = useState(false);
  const business = data?.business;
  const admin = data?.admin;

  if (error && !data) {
    return <ErrorAlert message={error} />;
  }

  return (
    <div className="page">
      <PageHeader
        compact
        eyebrow="CLUSTER DISPATCH"
        title="调度计划与集群元数据"
        description="查看业务面/管理面集群元数据、调度执行计划与执行明细。"
        visual={<PartitionVisual columns={3} leaderRows={[0, 1, 2]} badge="调度执行 · 计划追踪" />}
        actions={<Button icon={<ReloadOutlined spin={loading} />} onClick={refresh}>刷新调度数据</Button>}
      />

      {error && <Alert type="warning" showIcon message={`部分数据拉取失败：${error}`} style={{ marginBottom: 14 }} />}

      <div className="dispatch-grid">
        <section className="panel dispatch-panel">
          <SectionHeader
            title="业务面集群元数据"
            description="分区拓扑与复制配置"
            icon={<SwapOutlined />}
            extra={business?.clusterMeta ? <Tag color="cyan">v{business.clusterMeta.version}</Tag> : undefined}
          />
          {business?.clusterMeta ? (
            <>
              <Descriptions column={2} size="small" bordered items={[
                { key: 'rf', label: '副本数', children: `${business.clusterMeta.replicationFactor}（当前 ${business.clusterMeta.currentReplicationFactor}）` },
                { key: 'pc', label: '分区数', children: business.clusterMeta.currentPartitionCount },
                { key: 'ct', label: '创建时间', children: time(business.clusterMeta.createTime) },
                { key: 'ut', label: '更新时间', children: time(business.clusterMeta.updateTime) },
              ]} />
              <Button className="dispatch-meta-button" size="small" onClick={() => setMetaOpen(true)}>查看目标拓扑明细</Button>
            </>
          ) : (
            <Empty description="暂无元数据" />
          )}
        </section>

        <section className="panel dispatch-panel">
          <SectionHeader
            title="管理面集群元数据"
            description="控制面单 RAFT 组"
            icon={<SwapOutlined />}
            extra={admin?.clusterMeta ? <Tag color="purple">v{admin.clusterMeta.version}</Tag> : undefined}
          />
          {admin?.clusterMeta ? (
            <Descriptions column={2} size="small" bordered items={[
              { key: 'rf', label: '副本数', children: `${admin.clusterMeta.replicationFactor}（当前 ${admin.clusterMeta.currentReplicationFactor}）` },
              { key: 'ut', label: '更新时间', children: time(admin.clusterMeta.updateTime) },
              { key: 'pm', label: '主成员', children: admin.clusterMeta.meta?.primaryMemberId ?? '-', span: 2 },
              { key: 'members', label: '成员优先级', children: admin.clusterMeta.meta?.partitionMembers?.map((m) => `${m.memberId}(${m.priority})`).join(' / ') ?? '-', span: 2 },
            ]} />
          ) : (
            <Empty description="暂无元数据" />
          )}
        </section>

        <section className="panel dispatch-panel">
          <SectionHeader
            title="业务面调度计划（最新）"
            description="在途与最近完结计划"
            extra={business?.dispatchPlan ? <StateTag state={business.dispatchPlan.dispatchState} /> : undefined}
          />
          {business?.dispatchPlan ? (
            <>
              <Descriptions column={2} size="small" bordered items={[
                { key: 'id', label: '计划 ID', children: <Typography.Text code>{business.dispatchPlan.dispatchPlanId}</Typography.Text> },
                { key: 'type', label: '类型', children: business.dispatchPlan.dispatchPlanType },
                { key: 'pc', label: '分区数', children: `${business.dispatchPlan.oldPartitionsCount} → ${business.dispatchPlan.expectPartitionsCount}` },
                { key: 'rf', label: '副本数', children: `${business.dispatchPlan.oldReplicationFactor} → ${business.dispatchPlan.expectReplicationFactor}` },
              ]} />
              {business.dispatchPlan.transferSourceId.length > 0 && (
                <div className="dispatch-transfer">
                  <Typography.Text type="secondary">迁移 source：</Typography.Text>
                  {business.dispatchPlan.transferSourceId.map((s) => (
                    <Tag key={s} color="cyan">{s}</Tag>
                  ))}
                </div>
              )}
              <div className="drawer-section-title"><strong>执行明细</strong><span>{business.dispatchPlan.executionPlan.length} 步</span></div>
              <div className="dispatch-executions-wrap">
                <ExecutionsTimeline executions={business.dispatchPlan.executionPlan} />
              </div>
            </>
          ) : (
            <Empty description="暂无调度计划" />
          )}
        </section>

        <section className="panel dispatch-panel">
          <SectionHeader
            title="管理面调度计划"
            description="控制面调度执行"
            extra={admin?.dispatchPlan ? <StateTag state={admin.dispatchPlan.dispatchState} /> : undefined}
          />
          {admin?.dispatchPlan ? (
            <>
              <Descriptions column={2} size="small" bordered items={[
                { key: 'id', label: '计划 ID', children: <Typography.Text code>{admin.dispatchPlan.dispatchPlanId}</Typography.Text> },
                { key: 'type', label: '类型', children: admin.dispatchPlan.dispatchPlanType },
                { key: 'rf', label: '副本数', children: `${admin.dispatchPlan.oldReplicationFactor} → ${admin.dispatchPlan.expectReplicationFactor}` },
                { key: 'init', label: '初始化', children: admin.dispatchPlan.initialize ? '是' : '否' },
              ]} />
              <div className="drawer-section-title"><strong>执行明细</strong><span>{admin.dispatchPlan.executionPlan.length} 步</span></div>
              <div className="dispatch-executions-wrap">
                <ExecutionsTimeline executions={admin.dispatchPlan.executionPlan} />
              </div>
            </>
          ) : (
            <Empty description="暂无调度计划" />
          )}
        </section>
      </div>

      <Drawer
        className="detail-drawer"
        title="业务面目标拓扑（meta / lastMeta）"
        width={620}
        open={metaOpen}
        onClose={() => setMetaOpen(false)}
      >
        {business?.clusterMeta ? (
          <div className="dispatch-meta-drawer">
            <div className="drawer-section-title"><strong>目标拓扑（meta）</strong><span>{business.clusterMeta.meta.length} 个分区</span></div>
            <PartitionMetaTable rows={business.clusterMeta.meta} />
            {business.clusterMeta.lastMeta.length > 0 && (
              <>
                <div className="drawer-section-title"><strong>上一拓扑（lastMeta）</strong><span>{business.clusterMeta.lastMeta.length} 个分区</span></div>
                <PartitionMetaTable rows={business.clusterMeta.lastMeta} />
              </>
            )}
          </div>
        ) : (
          <Empty description="暂无元数据" />
        )}
      </Drawer>

      <p className="dispatch-note">
        调度计划存储保留在途与最近完结两份（前任计划在下一份完结时回收）；source 台账（分区 resource id）见分区矩阵 Source ID 列。
      </p>
    </div>
  );
}
