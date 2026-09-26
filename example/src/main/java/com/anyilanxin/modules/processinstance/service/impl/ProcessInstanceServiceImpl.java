package com.anyilanxin.modules.processinstance.service.impl;

import static com.anyilanxin.core.BaseService.getPage;
import static com.anyilanxin.core.BaseService.toPageData;
import static com.anyilanxin.core.config.SocketDestinationPrefixes.QUEUE_PROCESS_INSTANCE;
import static com.anyilanxin.core.config.SocketDestinationPrefixes.USER;

import com.anyilanxin.core.*;
import com.anyilanxin.core.exception.AnYiResponseException;
import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.processinstance.CreateProcessInstanceCommand;
import com.anyilanxin.modules.common.mapper.ActivityInstanceMapper;
import com.anyilanxin.modules.processinstance.controller.dto.ProcessInstanceCreateDto;
import com.anyilanxin.modules.processinstance.controller.dto.ProcessInstanceDto;
import com.anyilanxin.modules.processinstance.controller.dto.ProcessInstancePageDto;
import com.anyilanxin.modules.processinstance.controller.dto.ProcessInstanceQueryDto;
import com.anyilanxin.modules.processinstance.entity.ProcessInstanceEntity;
import com.anyilanxin.modules.processinstance.mapper.ProcessInstanceMapper;
import com.anyilanxin.modules.processinstance.service.IProcessInstanceService;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstanceBpmnBaseInfo;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstanceFlowVo;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstancePageVo;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstanceVo;
import com.anyilanxin.modules.resource.entity.ProcessDefinitionEntity;
import com.anyilanxin.modules.resource.mapper.ProcessDefinitionMapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.linpeilie.Converter;
import io.netty.util.HashedWheelTimer;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * 流程实例信息(ProcessInstance)业务层实现
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-22 15:11:53
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ProcessInstanceServiceImpl
    extends ServiceImpl<ProcessInstanceMapper, ProcessInstanceEntity>
    implements IProcessInstanceService {
  private final Converter converter;
  private final ProcessInstanceMapper processInstanceMapper;
  private final ProcessDefinitionMapper definitionMapper;
  private final KunpengClient client;
  private final SimpMessagingTemplate template;
  private final ActivityInstanceMapper activityInstanceMapper;
  private final HashedWheelTimer wheelTimer;
  private final Map<String, TimerTaskInfo> timerTaskMap = new ConcurrentHashMap<>();

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void save(final ProcessInstanceDto dto) throws RuntimeException {
    final var entity = converter.convert(dto, ProcessInstanceEntity.class);
    final var result = super.save(entity);
    if (!result) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "保存数据失败");
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void updateById(final String processInstanceId, final ProcessInstanceDto dto)
      throws RuntimeException {
    // 查询数据是否存在
    getById(processInstanceId);
    // 更新数据
    final var entity = converter.convert(dto, ProcessInstanceEntity.class);
    entity.setProcessInstanceId(processInstanceId);
    final var result = super.updateById(entity);
    if (!result) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "更新数据失败");
    }
  }

  @Override
  public List<ProcessInstanceVo> selectListByModel(final ProcessInstanceQueryDto dto)
      throws RuntimeException {
    final var list = processInstanceMapper.selectListByModel(dto);
    if (list == null || list.isEmpty()) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "未找到符合条件数据");
    }
    return list;
  }

  @Override
  public AnYiPageResult<ProcessInstancePageVo> pageByModel(final ProcessInstancePageDto dto)
      throws RuntimeException {
    return toPageData(processInstanceMapper.pageByModel(getPage(dto), dto));
  }

  @Override
  public ProcessInstanceVo getById(final String processInstanceId) throws RuntimeException {
    final var byId = super.getById(processInstanceId);
    if (Objects.isNull(byId)) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "未找到符合条件数据");
    }
    return converter.convert(byId, ProcessInstanceVo.class);
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void deleteById(final String processInstanceId) throws RuntimeException {
    // 查询数据是否存在
    getById(processInstanceId);
    // 删除数据
    final var b = removeById(processInstanceId);
    if (!b) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "删除数据失败");
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void deleteBatch(final List<String> processInstanceIds) throws RuntimeException {
    final var entities = listByIds(processInstanceIds);
    if (entities == null || entities.isEmpty()) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "数据不存在或已经被别人删除");
    }
    final var waitDeleteList = new ArrayList<String>();
    entities.forEach(v -> waitDeleteList.add(v.getProcessInstanceId()));
    final var i = processInstanceMapper.deleteByIds(waitDeleteList);
    if (i <= 0) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "批量删除成功");
    }
  }

  @Override
  public String create(final ProcessInstanceCreateDto dto) {
    final CreateProcessInstanceCommand.CreateProcessInstanceCommandStep3 commandStep3;
    if (dto.getProcessDefinitionId() != null) {
      commandStep3 =
          client
              .newCreateProcessInstanceCommand()
              .processDefinitionId(dto.getProcessDefinitionId());
    } else {
      final CreateProcessInstanceCommand.CreateProcessInstanceCommandStep2 commandStep2 =
          client
              .newCreateProcessInstanceCommand()
              .processDefinitionKey(dto.getProcessDefinitionKey());
      if (dto.getProcessDefinitionVersion() != null) {
        commandStep3 = commandStep2.version(dto.getProcessDefinitionVersion());
      } else {
        commandStep3 = commandStep2.latestVersion();
      }
    }
    if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
      commandStep3.variables(dto.getVariables());
    }
    return commandStep3.send().join().getProcessInstanceId() + "";
  }

  @Override
  public void cancel(final String processInstanceId) {
    client
        .newCancelProcessInstanceCommand()
        .processInstanceId(Long.parseLong(processInstanceId))
        .send()
        .join();
  }

  @Override
  public ProcessInstanceFlowVo subscribeProcessInstanceChange(final String processInstanceId) {
    return new ProcessInstanceFlowVo();
  }

  @EventListener
  public void handleDisconnectEvent(final SessionDisconnectEvent event) {
    final String sessionId = event.getSessionId();
    final Map<String, TimerTaskInfo> values = new HashMap<>(timerTaskMap);
    timerTaskMap.clear();
    final Set<Map.Entry<String, TimerTaskInfo>> entries = values.entrySet();
    for (final Map.Entry<String, TimerTaskInfo> entry : entries) {
      final String key = entry.getKey();
      final TimerTaskInfo timerTaskInfo = entry.getValue();
      if (key.startsWith(sessionId)) {
        timerTaskInfo.close();
      } else {
        timerTaskMap.put(key, timerTaskInfo);
      }
    }
  }

  @EventListener(condition = "#event.simpleTopic=='" + USER + QUEUE_PROCESS_INSTANCE + "'")
  public void handleUnsubscribe(final UnsubscribeEvent event) {
    final String subscriptionId = event.getSubscriptionId();
    final String topic = event.getTopic();
    final String sessionId = event.getSessionId();
    final Principal user = event.getUser();
    log.debug("\n❌ 取消订阅，topic：{},subscriptionId：{}", topic, subscriptionId);
    if (user != null) {
      final TimerTaskInfo oldTimerTask =
          timerTaskMap.get(sessionId + user.getName() + subscriptionId);
      if (oldTimerTask != null) {
        oldTimerTask.close();
      }
    }
  }

  @EventListener(condition = "#event.simpleTopic=='" + USER + QUEUE_PROCESS_INSTANCE + "'")
  public void handleSubscribe(final SubscribeEvent event) {
    final String subscriptionId = event.getSubscriptionId();
    final String topic = event.getTopic();
    final String sessionId = event.getSessionId();
    log.debug("\n✅ 订阅，topic：{},subscriptionId：{}", topic, subscriptionId);
    final Principal user = event.getUser();
    if (user != null && topic != null) {
      final ProcessInstanceTimerTask timerTask =
          new ProcessInstanceTimerTask(
              processInstanceMapper,
              activityInstanceMapper,
              template,
              topic,
              subscriptionId,
              user.getName(),
              wheelTimer,
              2,
              TimeUnit.SECONDS);
      final TimerTaskInfo oldTimerTask =
          timerTaskMap.get(sessionId + user.getName() + subscriptionId);
      if (oldTimerTask != null) {
        oldTimerTask.close();
      }
      timerTaskMap.put(sessionId + user.getName() + subscriptionId, timerTask);
      timerTask.cycleStart();
    }
  }

  @Override
  public ProcessInstanceBpmnBaseInfo queryBaseInfo(final String processInstanceId) {
    final ProcessInstanceEntity byId = super.getById(processInstanceId);
    if (byId == null) {
      throw new AnYiResponseException("流程实例不存在:" + processInstanceId);
    }
    final ProcessInstanceBpmnBaseInfo bpmnBaseInfo =
        converter.convert(byId, ProcessInstanceBpmnBaseInfo.class);
    final ProcessDefinitionEntity processDefinitionEntity =
        definitionMapper.selectById(bpmnBaseInfo.getProcessDefinitionId());
    if (processDefinitionEntity == null) {
      throw new AnYiResponseException("流程定义不存在:" + bpmnBaseInfo.getProcessDefinitionId());
    }
    bpmnBaseInfo.setBpmnData(processDefinitionEntity.getBpmnXml());
    return bpmnBaseInfo;
  }
}
