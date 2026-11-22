import { api, dispatchBalance, dispatchChangePartitions, dispatchChangeReplication, time } from '../api';
import { usePolling } from '../usePolling';
import { ErrorAlert } from '../components/ErrorAlert';
import { StateTag } from '../components/StatusBadge';
import PageHeader from '../components/PageHeader';
import SectionHeader from '../components/SectionHeader';
import PartitionVisual from '../components/PartitionVisual';
import type { DispatchExecutionVo, DispatchOpVo, DispatchVo } from '../types';
import { Alert, App, Button, Descriptions, Drawer, Empty, InputNumber, Table, Tag, Typography } from 'antd';
import { CaretRightOutlined, EyeOutlined, SwapOutlined } from '@ant-design/icons';
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
  const { data, error, refresh } = usePolling<DispatchVo>(() => api<DispatchVo>('/dispatch'));
  const [metaOpen, setMetaOpen] = useState(false);
  const [replication, setReplication] = useState<number | null>(null);
  const [partitions, setPartitions] = useState<number | null>(null);
  const [opResult, setOpResult] = useState<DispatchOpVo | null>(null);
  const [opLoading, setOpLoading] = useState<string | null>(null);
  const { modal, message } = App.useApp();
  const business = data?.business;
  const admin = data?.admin;

  if (error && !data) {
    return <ErrorAlert message={error} />;
  }

  const runOp = async (key: string, action: () => Promise<DispatchOpVo>, applying: boolean) => {
    setOpLoading(key);
    try {
      const result = await action();
      setOpResult(result);
      if (result.success) {
        message.success(applying ? `调度计划已下发执行（#${result.dispatchPlan?.dispatchPlanId ?? '-'}）` : '调度计划预览已生成');
        if (applying) {
          await refresh();
        }
      } else {
        message.error(`操作失败：${result.message}`);
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : '操作失败');
    } finally {
      setOpLoading(null);
    }
  };

  const withConfirm = (key: string, title: string, action: () => Promise<DispatchOpVo>) => {
    modal.confirm({
      title,
      content: '将生成调度计划并下发执行，分区与副本会按计划迁移。确定继续？',
      okText: '确认执行',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => runOp(key, action, true),
    });
  };

  return (
    <div className="page">
      <PageHeader
        compact
        eyebrow="CLUSTER DISPATCH"
        title="调度计划与集群元数据"
        description="查看业务面/管理面集群元数据、调度执行计划与执行明细。"
        visual={<PartitionVisual columns={3} leaderRows={[0, 1, 2]} badge="调度执行 · 计划追踪" />}
      />

      {error && <Alert type="warning" showIcon message={`部分数据拉取失败：${error}`} style={{ marginBottom: 14 }} />}

      <section className="panel dispatch-panel">
        <SectionHeader
          title="集群扩缩容"
          description="预览仅生成调度计划不下发；执行需二次确认，结果见下方计划区"
          icon={<SwapOutlined />}
          extra={business?.clusterMeta && <Tag color="cyan">当前 {business.clusterMeta.currentPartitionCount} 分区 · {business.clusterMeta.currentReplicationFactor} 副本</Tag>}
        />
        <div className="dispatch-ops">
          <div className="dispatch-op">
            <span className="dispatch-op__label">重新负载</span>
            <span className="dispatch-op__hint">按当前节点重新均衡分区归属</span>
            <Button size="small" icon={<EyeOutlined />} loading={opLoading === 'balance-preview'} onClick={() => runOp('balance-preview', () => dispatchBalance(false), false)}>预览计划</Button>
            <Button size="small" danger type="primary" icon={<CaretRightOutlined />} loading={opLoading === 'balance-apply'} onClick={() => withConfirm('balance-apply', '执行业务集群重新负载？', () => dispatchBalance(true))}>执行</Button>
          </div>
          <div className="dispatch-op">
            <span className="dispatch-op__label">副本因子</span>
            <InputNumber size="small" min={1} max={7} value={replication} onChange={setReplication} placeholder="目标副本数" style={{ width: 110 }} />
            <Button size="small" icon={<EyeOutlined />} disabled={!replication} loading={opLoading === 'replication-preview'} onClick={() => replication && runOp('replication-preview', () => dispatchChangeReplication(replication, false), false)}>预览计划</Button>
            <Button size="small" danger type="primary" icon={<CaretRightOutlined />} disabled={!replication} loading={opLoading === 'replication-apply'} onClick={() => replication && withConfirm('replication-apply', `执行副本因子变更为 ${replication}？`, () => dispatchChangeReplication(replication, true))}>执行</Button>
          </div>
          <div className="dispatch-op">
            <span className="dispatch-op__label">分区数量</span>
            <InputNumber size="small" min={1} max={64} value={partitions} onChange={setPartitions} placeholder="目标分区数" style={{ width: 110 }} />
            <Button size="small" icon={<EyeOutlined />} disabled={!partitions} loading={opLoading === 'partitions-preview'} onClick={() => partitions && runOp('partitions-preview', () => dispatchChangePartitions(partitions, false), false)}>预览计划</Button>
            <Button size="small" danger type="primary" icon={<CaretRightOutlined />} disabled={!partitions} loading={opLoading === 'partitions-apply'} onClick={() => partitions && withConfirm('partitions-apply', `执行分区数量变更为 ${partitions}？`, () => dispatchChangePartitions(partitions, true))}>执行</Button>
          </div>
        </div>
        {opResult && (
          <div className={`dispatch-op-result ${opResult.success ? '' : 'dispatch-op-result--error'}`}>
            <div className="dispatch-op-result__head">
              <strong>{opResult.operation}</strong>
              <StateTag state={opResult.success ? 'SUCCEED' : 'FAILED'} />
              {opResult.dispatchPlan && <Tag color="blue">计划 #{opResult.dispatchPlan.dispatchPlanId}</Tag>}
              {opResult.dispatchPlan && <Tag>{opResult.dispatchPlan.dispatchPlanType}</Tag>}
              {opResult.dispatchPlan && <Tag>{opResult.dispatchPlan.dispatchState}</Tag>}
              <span>{opResult.message}</span>
            </div>
            {opResult.dispatchPlan && (
              <span className="dispatch-op-result__meta">
                分区 {opResult.dispatchPlan.oldPartitionsCount} → {opResult.dispatchPlan.expectPartitionsCount} · 副本 {opResult.dispatchPlan.oldReplicationFactor} → {opResult.dispatchPlan.expectReplicationFactor} · {opResult.dispatchPlan.applyPlan ? '已下发执行' : '仅预览'}
              </span>
            )}
          </div>
        )}
      </section>

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
