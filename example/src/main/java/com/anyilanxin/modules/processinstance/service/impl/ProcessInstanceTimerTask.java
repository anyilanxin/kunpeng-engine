/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.modules.processinstance.service.impl;

import static com.anyilanxin.core.config.SocketDestinationPrefixes.USER;

import com.anyilanxin.core.TimerTaskInfo;
import com.anyilanxin.modules.common.entity.ActivityInstanceEntity;
import com.anyilanxin.modules.common.mapper.ActivityInstanceMapper;
import com.anyilanxin.modules.processinstance.mapper.ProcessInstanceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.netty.util.HashedWheelTimer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Strings;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * @author zxuanhong
 * @date 2026-04-23 12:19
 * @since
 */
public class ProcessInstanceTimerTask extends TimerTaskInfo {
  private final ProcessInstanceMapper processInstanceMapper;
  private final ActivityInstanceMapper activityInstanceMapper;
  private final SimpMessagingTemplate template;
  private final String subscriptionId;
  private final String topic;
  private final String userId;
  private final long processInstanceId;
  private Long maxActivityInstanceId;
  private Long minNotCompleteActivityInstanceId;
  private LocalDateTime maxCompleteEndTime;
  private final Map<Long, Integer> activityInstanceHash = new HashMap<>();
  private final Map<String, Object> subscriptionHeader = new HashMap<>();

  public ProcessInstanceTimerTask(
      final ProcessInstanceMapper processInstanceMapper,
      final ActivityInstanceMapper activityInstanceMapper,
      final SimpMessagingTemplate template,
      final String topic,
      final String subscriptionId,
      final String userId,
      final HashedWheelTimer wheelTimer,
      final long delay,
      final TimeUnit unit) {
    super(wheelTimer, delay, unit);
    this.topic = Strings.CS.removeStart(topic, USER);
    processInstanceId = Long.parseLong(topic.substring(topic.lastIndexOf("/") + 1));
    this.userId = userId;
    this.processInstanceMapper = processInstanceMapper;
    this.activityInstanceMapper = activityInstanceMapper;
    this.template = template;
    this.subscriptionId = subscriptionId;
    subscriptionHeader.put("id", subscriptionId);
  }

  @Override
  public void process() {
    final Map<String, ActivityInstanceEntity> allActivityInstance = new HashMap<>();
    if (maxActivityInstanceId == null && minNotCompleteActivityInstanceId == null) {
      final LambdaQueryWrapper<ActivityInstanceEntity> lambdaQueryWrapper =
          new LambdaQueryWrapper<>();
      lambdaQueryWrapper.eq(ActivityInstanceEntity::getProcessInstanceId, processInstanceId);
      final List<ActivityInstanceEntity> activityInstanceEntities =
          activityInstanceMapper.selectList(lambdaQueryWrapper);
      if (!activityInstanceEntities.isEmpty()) {
        activityInstanceEntities.forEach(
            v -> {
              allActivityInstance.put(v.getActivityInstanceId(), v);
            });
      }
    } else {
      if (maxActivityInstanceId != null) {
        final LambdaQueryWrapper<ActivityInstanceEntity> lambdaQueryWrapper =
            new LambdaQueryWrapper<>();
        lambdaQueryWrapper
            .eq(ActivityInstanceEntity::getProcessInstanceId, processInstanceId)
            .gt(ActivityInstanceEntity::getActivityInstanceId, maxActivityInstanceId);
        final List<ActivityInstanceEntity> activityInstanceEntities =
            activityInstanceMapper.selectList(lambdaQueryWrapper);
        if (!activityInstanceEntities.isEmpty()) {
          activityInstanceEntities.forEach(
              v -> {
                allActivityInstance.put(v.getActivityInstanceId(), v);
              });
        }
      }
      if (minNotCompleteActivityInstanceId != null) {
        final LambdaQueryWrapper<ActivityInstanceEntity> lambdaQueryWrapper =
            new LambdaQueryWrapper<>();
        lambdaQueryWrapper
            .eq(ActivityInstanceEntity::getProcessInstanceId, processInstanceId)
            .and(
                v ->
                    v.and(
                            sv ->
                                sv.ge(
                                        ActivityInstanceEntity::getActivityInstanceId,
                                        minNotCompleteActivityInstanceId)
                                    .ge(ActivityInstanceEntity::getEndTime, maxCompleteEndTime))
                        .or()
                        .isNull(ActivityInstanceEntity::getEndTime));
        final List<ActivityInstanceEntity> activityInstanceEntities =
            activityInstanceMapper.selectList(lambdaQueryWrapper);
        if (!activityInstanceEntities.isEmpty()) {
          activityInstanceEntities.forEach(
              v -> allActivityInstance.put(v.getActivityInstanceId(), v));
        } else {
          minNotCompleteActivityInstanceId = null;
        }
      }
    }
    final List<ActivityInstanceEntity> activityInstanceEntities =
        new ArrayList<>(allActivityInstance.values());
    boolean haveNotComplete = false;
    if (!activityInstanceEntities.isEmpty()) {
      final List<ActivityInstanceEntity> pushActivityInstance =
          new ArrayList<>(activityInstanceEntities.size());
      activityInstanceEntities.sort(
          Comparator.comparingLong(o -> Long.parseLong(o.getSequenceCounter())));
      for (final ActivityInstanceEntity instanceEntity : activityInstanceEntities) {
        final Long activityInstanceId = Long.parseLong(instanceEntity.getActivityInstanceId());
        final Integer oldHashCode = activityInstanceHash.get(activityInstanceId);
        final int hashCode = instanceEntity.hashCode();
        boolean pushMessage = false;
        if (maxActivityInstanceId == null) {
          maxActivityInstanceId = activityInstanceId;
        } else if (activityInstanceId > maxActivityInstanceId) {
          maxActivityInstanceId = activityInstanceId;
        }
        if (instanceEntity.getEndTime() == null) {
          haveNotComplete = true;
          if (minNotCompleteActivityInstanceId == null) {
            minNotCompleteActivityInstanceId = activityInstanceId;
          } else if (activityInstanceId < minNotCompleteActivityInstanceId) {
            minNotCompleteActivityInstanceId = activityInstanceId;
          }
        }
        if (instanceEntity.getEndTime() != null) {
          if (maxCompleteEndTime == null) {
            maxCompleteEndTime = instanceEntity.getEndTime();
          } else {
            if (!maxCompleteEndTime.isAfter(instanceEntity.getEndTime())) {
              maxCompleteEndTime = instanceEntity.getEndTime();
            }
          }
        }

        if (oldHashCode == null) {
          pushMessage = true;
        } else {
          if (oldHashCode != hashCode) {
            pushMessage = true;
          }
        }
        if (pushMessage) {
          pushActivityInstance.add(instanceEntity);
          activityInstanceHash.put(activityInstanceId, hashCode);
        }
      }
      if (!pushActivityInstance.isEmpty()) {
        template.convertAndSendToUser(userId, topic, pushActivityInstance, subscriptionHeader);
      }
    }
    if (!haveNotComplete) {
      close();
    }
  }
}
