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
import { useTranslation } from 'react-i18next';
import { useState } from 'react';

function ExecutionsTimeline({ executions }: { executions: DispatchExecutionVo[] }) {
  const { t } = useTranslation();
  if (!executions.length) {
    return <Typography.Text type="secondary">{t('app.dispatch.timelineNoData')}</Typography.Text>;
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
              <span>{t('app.dispatch.startEnd', { start: time(e.startTime), end: time(e.endTime) })}</span>
              {e.remaining >= 0 && <span>{t('app.dispatch.remaining', { count: e.remaining })}</span>}
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
  const { t } = useTranslation();
  return (
    <Table
      rowKey={(r) => `${r.partitionGroup}:${r.partitionId}`}
      size="small"
      dataSource={rows ?? []}
      pagination={false}
      columns={[
        { title: t('app.dispatch.tableGroup'), dataIndex: 'partitionGroup' },
        { title: t('app.dispatch.tablePartition'), dataIndex: 'partitionId', render: (v: number) => `#${v}` },
        { title: t('app.dispatch.tablePrimary'), dataIndex: 'primaryMemberId' },
        {
          title: t('app.dispatch.tableMembers'),
          dataIndex: 'partitionMembers',
          render: (ms: MetaRow['partitionMembers']) =>
            ms.map((m) => `${m.memberId}(${m.priority})`).join(' / '),
        },
      ]}
    />
  );
}

export default function DispatchPage() {
  const { t } = useTranslation();
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
        message.success(applying ? t('app.dispatch.appliedToast', { id: result.dispatchPlan?.dispatchPlanId ?? '-' }) : t('app.dispatch.previewToast'));
        if (applying) {
          await refresh();
        }
      } else {
        message.error(t('app.dispatch.opFailed', { message: result.message }));
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : t('app.dispatch.opFailedGeneric'));
    } finally {
      setOpLoading(null);
    }
  };

  const withConfirm = (key: string, title: string, action: () => Promise<DispatchOpVo>) => {
    modal.confirm({
      title,
      content: t('app.dispatch.confirmContent'),
      okText: t('app.dispatch.confirmOk'),
      okButtonProps: { danger: true },
      cancelText: t('app.common.cancel'),
      onOk: () => runOp(key, action, true),
    });
  };

  /** 业务面/管理面元数据共有的展示字段；管理面无分区数与创建时间，用 withCreate 区分 */
  const metaItemsOf = (cluster: { replicationFactor: number; currentReplicationFactor: number; currentPartitionCount?: number; createTime: number; updateTime: number }, withCreate: boolean) => [
    { key: 'rf', label: t('app.dispatch.labelReplication'), children: `${cluster.replicationFactor}${t('app.dispatch.currentInParens', { value: cluster.currentReplicationFactor })}` },
    ...(withCreate ? [{ key: 'pc', label: t('app.dispatch.labelPartitions'), children: cluster.currentPartitionCount }] : []),
    ...(withCreate ? [{ key: 'ct', label: t('app.dispatch.labelCreateTime'), children: time(cluster.createTime) }] : []),
    { key: 'ut', label: t('app.dispatch.labelUpdateTime'), children: time(cluster.updateTime) },
  ];

  return (
    <div className="page">
      <PageHeader
        compact
        eyebrow="CLUSTER DISPATCH"
        title={t('app.dispatch.heroTitle')}
        description={t('app.dispatch.heroDesc')}
        visual={<PartitionVisual columns={3} leaderRows={[0, 1, 2]} badge={t('app.dispatch.visualBadge')} />}
      />

      {error && <Alert type="warning" showIcon message={t('app.common.partialError', { error })} style={{ marginBottom: 14 }} />}

      <section className="panel dispatch-panel">
        <SectionHeader
          title={t('app.dispatch.opsTitle')}
          description={t('app.dispatch.opsDesc')}
          icon={<SwapOutlined />}
          extra={business?.clusterMeta && <Tag color="cyan">{t('app.dispatch.currentTag', { partitions: business.clusterMeta.currentPartitionCount, replication: business.clusterMeta.currentReplicationFactor })}</Tag>}
        />
        <div className="dispatch-ops">
          <div className="dispatch-op">
            <span className="dispatch-op__label">{t('app.dispatch.opBalance')}</span>
            <span className="dispatch-op__hint">{t('app.dispatch.opBalanceHint')}</span>
            <Button size="small" icon={<EyeOutlined />} loading={opLoading === 'balance-preview'} onClick={() => runOp('balance-preview', () => dispatchBalance(false), false)}>{t('app.dispatch.previewPlan')}</Button>
            <Button size="small" danger type="primary" icon={<CaretRightOutlined />} loading={opLoading === 'balance-apply'} onClick={() => withConfirm('balance-apply', t('app.dispatch.confirmBalance'), () => dispatchBalance(true))}>{t('app.dispatch.execute')}</Button>
          </div>
          <div className="dispatch-op">
            <span className="dispatch-op__label">{t('app.dispatch.opReplication')}</span>
            <InputNumber size="small" min={1} max={7} value={replication} onChange={setReplication} placeholder={t('app.dispatch.placeholderReplication')} style={{ width: 110 }} />
            <Button size="small" icon={<EyeOutlined />} disabled={!replication} loading={opLoading === 'replication-preview'} onClick={() => replication && runOp('replication-preview', () => dispatchChangeReplication(replication, false), false)}>{t('app.dispatch.previewPlan')}</Button>
            <Button size="small" danger type="primary" icon={<CaretRightOutlined />} disabled={!replication} loading={opLoading === 'replication-apply'} onClick={() => replication && withConfirm('replication-apply', t('app.dispatch.confirmReplication', { value: replication }), () => dispatchChangeReplication(replication, true))}>{t('app.dispatch.execute')}</Button>
          </div>
          <div className="dispatch-op">
            <span className="dispatch-op__label">{t('app.dispatch.opPartitions')}</span>
            <InputNumber size="small" min={1} max={64} value={partitions} onChange={setPartitions} placeholder={t('app.dispatch.placeholderPartitions')} style={{ width: 110 }} />
            <Button size="small" icon={<EyeOutlined />} disabled={!partitions} loading={opLoading === 'partitions-preview'} onClick={() => partitions && runOp('partitions-preview', () => dispatchChangePartitions(partitions, false), false)}>{t('app.dispatch.previewPlan')}</Button>
            <Button size="small" danger type="primary" icon={<CaretRightOutlined />} disabled={!partitions} loading={opLoading === 'partitions-apply'} onClick={() => partitions && withConfirm('partitions-apply', t('app.dispatch.confirmPartitions', { value: partitions }), () => dispatchChangePartitions(partitions, true))}>{t('app.dispatch.execute')}</Button>
          </div>
        </div>
        {opResult && (
          <div className={`dispatch-op-result ${opResult.success ? '' : 'dispatch-op-result--error'}`}>
            <div className="dispatch-op-result__head">
              <strong>{opResult.operation}</strong>
              <StateTag state={opResult.success ? 'SUCCEED' : 'FAILED'} />
              {opResult.dispatchPlan && <Tag color="blue">{t('app.dispatch.planTag', { id: opResult.dispatchPlan.dispatchPlanId })}</Tag>}
              {opResult.dispatchPlan && <Tag>{opResult.dispatchPlan.dispatchPlanType}</Tag>}
              {opResult.dispatchPlan && <Tag>{opResult.dispatchPlan.dispatchState}</Tag>}
              <span>{opResult.message}</span>
            </div>
            {opResult.dispatchPlan && (
              <span className="dispatch-op-result__meta">
                {t('app.dispatch.resultMeta', {
                  oldPc: opResult.dispatchPlan.oldPartitionsCount,
                  expectPc: opResult.dispatchPlan.expectPartitionsCount,
                  oldRf: opResult.dispatchPlan.oldReplicationFactor,
                  expectRf: opResult.dispatchPlan.expectReplicationFactor,
                  mode: opResult.dispatchPlan.applyPlan ? t('app.dispatch.applied') : t('app.dispatch.previewOnly'),
                })}
              </span>
            )}
          </div>
        )}
      </section>

      <div className="dispatch-grid">
        <section className="panel dispatch-panel">
          <SectionHeader
            title={t('app.dispatch.businessMetaTitle')}
            description={t('app.dispatch.businessMetaDesc')}
            icon={<SwapOutlined />}
            extra={business?.clusterMeta ? <Tag color="cyan">v{business.clusterMeta.version}</Tag> : undefined}
          />
          {business?.clusterMeta ? (
            <>
              <Descriptions column={2} size="small" bordered items={metaItemsOf(business.clusterMeta, true)} />
              <Button className="dispatch-meta-button" size="small" onClick={() => setMetaOpen(true)}>{t('app.dispatch.viewTopology')}</Button>
            </>
          ) : (
            <Empty description={t('app.dispatch.emptyMeta')} />
          )}
        </section>

        <section className="panel dispatch-panel">
          <SectionHeader
            title={t('app.dispatch.adminMetaTitle')}
            description={t('app.dispatch.adminMetaDesc')}
            icon={<SwapOutlined />}
            extra={admin?.clusterMeta ? <Tag color="purple">v{admin.clusterMeta.version}</Tag> : undefined}
          />
          {admin?.clusterMeta ? (
            <Descriptions column={2} size="small" bordered items={[
              ...metaItemsOf(admin.clusterMeta, false),
              { key: 'pm', label: t('app.dispatch.labelPrimary'), children: admin.clusterMeta.meta?.primaryMemberId ?? '-', span: 2 },
              { key: 'members', label: t('app.dispatch.labelMemberPriority'), children: admin.clusterMeta.meta?.partitionMembers?.map((m) => `${m.memberId}(${m.priority})`).join(' / ') ?? '-', span: 2 },
            ]} />
          ) : (
            <Empty description={t('app.dispatch.emptyMeta')} />
          )}
        </section>

        <section className="panel dispatch-panel">
          <SectionHeader
            title={t('app.dispatch.businessPlanTitle')}
            description={t('app.dispatch.businessPlanDesc')}
            extra={business?.dispatchPlan ? <StateTag state={business.dispatchPlan.dispatchState} /> : undefined}
          />
          {business?.dispatchPlan ? (
            <>
              <Descriptions column={2} size="small" bordered items={[
                { key: 'id', label: t('app.dispatch.labelPlanId'), children: <Typography.Text code>{business.dispatchPlan.dispatchPlanId}</Typography.Text> },
                { key: 'type', label: t('app.dispatch.labelType'), children: business.dispatchPlan.dispatchPlanType },
                { key: 'pc', label: t('app.dispatch.labelPartitions'), children: `${business.dispatchPlan.oldPartitionsCount} → ${business.dispatchPlan.expectPartitionsCount}` },
                { key: 'rf', label: t('app.dispatch.labelReplication'), children: `${business.dispatchPlan.oldReplicationFactor} → ${business.dispatchPlan.expectReplicationFactor}` },
              ]} />
              {business.dispatchPlan.transferSourceId.length > 0 && (
                <div className="dispatch-transfer">
                  <Typography.Text type="secondary">{t('app.dispatch.transferSource')}</Typography.Text>
                  {business.dispatchPlan.transferSourceId.map((s) => (
                    <Tag key={s} color="cyan">{s}</Tag>
                  ))}
                </div>
              )}
              <div className="drawer-section-title"><strong>{t('app.dispatch.executions')}</strong><span>{t('app.dispatch.steps', { count: business.dispatchPlan.executionPlan.length })}</span></div>
              <div className="dispatch-executions-wrap">
                <ExecutionsTimeline executions={business.dispatchPlan.executionPlan} />
              </div>
            </>
          ) : (
            <Empty description={t('app.dispatch.emptyPlan')} />
          )}
        </section>

        <section className="panel dispatch-panel">
          <SectionHeader
            title={t('app.dispatch.adminPlanTitle')}
            description={t('app.dispatch.adminPlanDesc')}
            extra={admin?.dispatchPlan ? <StateTag state={admin.dispatchPlan.dispatchState} /> : undefined}
          />
          {admin?.dispatchPlan ? (
            <>
              <Descriptions column={2} size="small" bordered items={[
                { key: 'id', label: t('app.dispatch.labelPlanId'), children: <Typography.Text code>{admin.dispatchPlan.dispatchPlanId}</Typography.Text> },
                { key: 'type', label: t('app.dispatch.labelType'), children: admin.dispatchPlan.dispatchPlanType },
                { key: 'rf', label: t('app.dispatch.labelReplication'), children: `${admin.dispatchPlan.oldReplicationFactor} → ${admin.dispatchPlan.expectReplicationFactor}` },
                { key: 'init', label: t('app.dispatch.labelInit'), children: admin.dispatchPlan.initialize ? t('app.common.yes') : t('app.common.no') },
              ]} />
              <div className="drawer-section-title"><strong>{t('app.dispatch.executions')}</strong><span>{t('app.dispatch.steps', { count: admin.dispatchPlan.executionPlan.length })}</span></div>
              <div className="dispatch-executions-wrap">
                <ExecutionsTimeline executions={admin.dispatchPlan.executionPlan} />
              </div>
            </>
          ) : (
            <Empty description={t('app.dispatch.emptyPlan')} />
          )}
        </section>
      </div>

      <Drawer
        className="detail-drawer"
        title={t('app.dispatch.metaDrawerTitle')}
        width={620}
        open={metaOpen}
        onClose={() => setMetaOpen(false)}
      >
        {business?.clusterMeta ? (
          <div className="dispatch-meta-drawer">
            <div className="drawer-section-title"><strong>{t('app.dispatch.targetTopology')}</strong><span>{t('app.dispatch.partitionCountUnit', { count: business.clusterMeta.meta.length })}</span></div>
            <PartitionMetaTable rows={business.clusterMeta.meta} />
            {business.clusterMeta.lastMeta.length > 0 && (
              <>
                <div className="drawer-section-title"><strong>{t('app.dispatch.lastTopology')}</strong><span>{t('app.dispatch.partitionCountUnit', { count: business.clusterMeta.lastMeta.length })}</span></div>
                <PartitionMetaTable rows={business.clusterMeta.lastMeta} />
              </>
            )}
          </div>
        ) : (
          <Empty description={t('app.dispatch.emptyMeta')} />
        )}
      </Drawer>

      <p className="dispatch-note">
        {t('app.dispatch.note')}
      </p>
    </div>
  );
}
