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
package com.anyilanxin.modules.job.service.impl;

import static com.anyilanxin.core.BaseService.getPage;
import static com.anyilanxin.core.BaseService.toPageData;

import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.AnYiResultStatus;
import com.anyilanxin.core.exception.AnYiResponseException;
import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.command.job.worker.JobClient;
import com.anyilanxin.kunpeng.client.command.message.correlation.MessageCorrelationCommand;
import com.anyilanxin.kunpeng.client.spring.annotation.JobWorker;
import com.anyilanxin.modules.job.controller.dto.JobCompleteDto;
import com.anyilanxin.modules.job.controller.dto.JobDto;
import com.anyilanxin.modules.job.controller.dto.JobPageDto;
import com.anyilanxin.modules.job.controller.dto.JobQueryDto;
import com.anyilanxin.modules.job.entity.JobEntity;
import com.anyilanxin.modules.job.mapper.JobMapper;
import com.anyilanxin.modules.job.service.IJobService;
import com.anyilanxin.modules.job.service.vo.JobPageVo;
import com.anyilanxin.modules.job.service.vo.JobVo;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.linpeilie.Converter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class JobServiceImpl extends ServiceImpl<JobMapper, JobEntity> implements IJobService {
  private final Converter converter;
  private final JobMapper mapper;
  private final KunpengClient client;

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void save(final JobDto dto) throws RuntimeException {
    final var entity = converter.convert(dto, JobEntity.class);
    final var result = super.save(entity);
    if (!result) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "保存数据失败");
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void updateById(final Long jobId, final JobDto dto) throws RuntimeException {
    // 查询数据是否存在
    getById(jobId);
    // 更新数据
    final var entity = converter.convert(dto, JobEntity.class);
    entity.setJobId(jobId);
    final var result = super.updateById(entity);
    if (!result) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "更新数据失败");
    }
  }

  @Override
  public List<JobVo> selectListByModel(final JobQueryDto dto) throws RuntimeException {
    final var list = mapper.selectListByModel(dto);
    if (list == null || list.isEmpty()) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "未找到符合条件数据");
    }
    return list;
  }

  @Override
  public AnYiPageResult<JobPageVo> pageByModel(final JobPageDto dto) throws RuntimeException {
    return toPageData(mapper.pageByModel(getPage(dto), dto));
  }

  @Override
  public JobVo getById(final Long jobId) throws RuntimeException {
    final var byId = super.getById(jobId);
    if (Objects.isNull(byId)) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "未找到符合条件数据");
    }
    return converter.convert(byId, JobVo.class);
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void deleteById(final Long jobId) throws RuntimeException {
    // 查询数据是否存在
    getById(jobId);
    // 删除数据
    final var b = removeById(jobId);
    if (!b) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "删除数据失败");
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void deleteBatch(final List<Long> jobIds) throws RuntimeException {
    final var entities = listByIds(jobIds);
    if (entities == null || entities.isEmpty()) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "数据不存在或已经被别人删除");
    }
    final var waitDeleteList = new ArrayList<Long>();
    entities.forEach(v -> waitDeleteList.add(v.getJobId()));
    final var i = mapper.deleteBatchIds(waitDeleteList);
    if (i <= 0) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "批量删除成功");
    }
  }

  @Override
  public void completeJob(final JobCompleteDto dto) {
    client.newCompleteCommand(dto.getJobId()).send().join();
  }

  //  @JobWorker(type = "process_start_one", name = "sdfsdfsdf")
  //  public void processStartOne(final ActivatedJob job, final JobClient jobClient) {
  //    System.out.println("-processStartOne-收到 work---" + job.getProcessInstanceId());
  //    jobClient.newCompleteCommand(job).send().join();
  //  }
  //
  //  @JobWorker(type = "process_start_two", name = "sdfsdfsdf")
  //  public void processStartWto(final ActivatedJob job, final JobClient jobClient) {
  //    System.out.println("-processStartWto-收到 work---" + job.getProcessInstanceId());
  //    jobClient.newCompleteCommand(job).send().join();
  //  }
  //
  //  @JobWorker(type = "process_end", name = "sdfsdfsdf")
  //  public void processEnd(final ActivatedJob job, final JobClient jobClient) {
  //    System.out.println("-processEnd-收到 work---" + job.getProcessInstanceId());
  //    jobClient.newCompleteCommand(job).send().join();
  //  }
  //
  //  @JobWorker(
  //      type = "execution_start",
  //      name = "sdfsdfsdf",
  //      fetchVariables = {"OutputVariable_0o9ajkh"})
  //  public void activityStart(final ActivatedJob job, final JobClient jobClient) {
  //    System.out.println("-activityStart-收到 work-instance--" + job.getProcessInstanceId());
  //    System.out.println("-activityStart-收到 work--fetchVariable-" + job.getVariablesAsMap());
  //    jobClient.newCompleteCommand(job).send().join();
  //  }
  //
  //  @JobWorker(
  //      streamEnabled = true,
  //      type = "execution_end",
  //      name = "sdfsdfsdf",
  //      fetchVariables = {"OutputVariable_0o9ajkh"})
  //  public void activityEnd(final ActivatedJob job, final JobClient jobClient) {
  //    System.out.println("-activityEnd-收到 work-instance--" + job.getProcessInstanceId());
  //    System.out.println("-activityEnd-收到 work--fetchVariable-" + job.getVariablesAsMap());
  //    jobClient.newCompleteCommand(job).send().join();
  //  }

  @JobWorker(
      type = "register_user",
      name = "qwwqw",
      fetchVariables = {"Output_userInfo"},
      streamEnabled = true)
  public void setUserInfo(final ActivatedJob job, final JobClient jobClient) {
    System.out.println("-setUserInfo-收到 work-instance--" + job.getProcessInstanceId());
    jobClient.newCompleteCommand(job).localVariable("userId", "zhouxuanhong111").send().join();
  }

  @JobWorker(type = "message_throw", name = "message", streamEnabled = true)
  public void messageThrow(final ActivatedJob job, final JobClient jobClient) {
    System.out.println("-message_throw--" + job.getProcessInstanceId());
    final MessageCorrelationCommand messageCorrelationCommand =
        client.newMessageCorrelationCommand();

    messageCorrelationCommand
        .messageName("Message_28bsmk6")
        .processInstanceId(job.getProcessInstanceId())
        .send()
        .join();

    jobClient.newCompleteCommand(job).localVariable("userId", "zhouxuanhong111").send().join();
  }
}
