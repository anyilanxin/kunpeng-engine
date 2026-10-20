package com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch;

import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventProcessorSingleState;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchPlanState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableBusinessRepository;
import java.util.List;
import org.slf4j.Logger;

/**
 * 业务面调度计划命令处理器抽象基类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class AbstractBusinessDispatchProcessor
    implements LogEventProcessorSingleState<BusinessDispatchPlanRecord> {
  protected final LogEventWriter writer;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;

  public AbstractBusinessDispatchProcessor(final LogEventWriter writer) {
    this.writer = writer;
  }

  /**
   * 计划单槽闸门（业务面独立计算，与管理面互不干扰）：永远最多保留一份未执行或执行中的业务计划。执行中
   * （EXECUTED）计划拒绝新调度；未应用（PLAN_DELAYED/PLAN_SUCCEED）计划发取消命令覆盖，由新计划接管单槽 （未应用计划的明细全为
   * WAIT，取消补偿空转天然安全）；终态计划不构成障碍。其余非终态取保守值按执行中拒绝。
   *
   * @param requestId 请求 id（拒绝时回错误响应）
   * @return true 继续创建新计划；false 已拒绝
   */
  protected boolean rejectOrReplaceInFlightPlan(final long requestId) {
    final ImmutableBusinessRepository repositoryBusiness =
        writer.getRepository().businessRepository();
    final List<BusinessDispatchPlanRecord> inFlightPlans =
        repositoryBusiness.getDispatchPlans().stream()
            .filter(
                plan ->
                    plan.getDispatchState() != DispatchPlanState.SUCCEED
                        && plan.getDispatchState() != DispatchPlanState.FAILED
                        && plan.getDispatchState() != DispatchPlanState.CANCELED)
            .toList();
    if (inFlightPlans.isEmpty()) {
      return true;
    }
    final boolean executing =
        inFlightPlans.stream()
            .anyMatch(
                plan ->
                    plan.getDispatchState() != DispatchPlanState.PLAN_DELAYED
                        && plan.getDispatchState() != DispatchPlanState.PLAN_SUCCEED);
    if (executing) {
      writer.adErrorResponse(requestId, -1, "业务面调度计划执行中，待执行完成后才能发起新调度（单槽互斥）");
      return false;
    }
    for (final BusinessDispatchPlanRecord unapplied : inFlightPlans) {
      LOGGER.info(
          "未应用调度计划被新调度覆盖，取消旧计划接管单槽。old plan id:{}，state:{}",
          unapplied.getDispatchPlanId(),
          unapplied.getDispatchState());
      writer.addCommand(
          unapplied.getDispatchPlanId(),
          BusinessDispatchPlanLifeCycle.CANCELING,
          requestId,
          unapplied);
    }
    return true;
  }

  @Override
  public AdminValueType valueType() {
    return AdminValueType.BUSINESS_DISPATCH;
  }

  @Override
  public abstract BusinessDispatchPlanLifeCycle valueLifeCycle();
}
