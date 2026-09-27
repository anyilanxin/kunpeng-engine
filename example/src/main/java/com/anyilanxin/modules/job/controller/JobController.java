/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.modules.job.controller;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.core.validation.annotation.NotNullSize;
import com.anyilanxin.core.validation.annotation.PathNotBlankOrNull;
import com.anyilanxin.modules.job.controller.dto.JobCompleteDto;
import com.anyilanxin.modules.job.controller.dto.JobDto;
import com.anyilanxin.modules.job.controller.dto.JobPageDto;
import com.anyilanxin.modules.job.controller.dto.JobQueryDto;
import com.anyilanxin.modules.job.service.IJobService;
import com.anyilanxin.modules.job.service.vo.JobPageVo;
import com.anyilanxin.modules.job.service.vo.JobVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@Tag(name = "Job", description = "作业信息相关")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@RequestMapping(value = "/job", produces = MediaType.APPLICATION_JSON_VALUE)
public class JobController extends AnYiBaseController {
  private final IJobService service;

  /**
   * 作业信息添加
   *
   * @param dto 待添加信息
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-05-13 16:45:42
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/insert")
  public AnYiResult<String> insert(@RequestBody @Valid final JobDto dto) {
    service.save(dto);
    return ok("保存成功");
  }

  /**
   * 完成作业
   *
   * @param dto 待完成作业的作业信息
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-05-13 16:45:42
   */
  @Operation(tags = {"v1.0.0"})
  @PostMapping(value = "/complete-job")
  public AnYiResult<String> completeJob(@RequestBody @Valid final JobCompleteDto dto) {
    service.completeJob(dto);
    return ok("完成作业成功");
  }

  /**
   * 通过作业id修改
   *
   * @param jobId 作业id
   * @param dto 待修改信息
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-05-13 16:45:42
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @Parameter(in = ParameterIn.PATH, description = "作业id", name = "jobId", required = true)
  @PutMapping(value = "/update/{jobId}")
  public AnYiResult<String> update(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "作业id不能为空") final Long jobId,
      @RequestBody @Valid final JobDto dto) {
    service.updateById(jobId, dto);
    return ok("更新成功");
  }

  /**
   * 作业信息逻辑删除
   *
   * @param jobId 作业id
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-05-13 16:45:42
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @Parameter(in = ParameterIn.PATH, description = "作业id", name = "jobId", required = true)
  @DeleteMapping(value = "/delete-one/{jobId}")
  public AnYiResult<String> deleteById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "作业id不能为空") final Long jobId) {
    service.deleteById(jobId);
    return ok("删除成功");
  }

  /**
   * 作业信息逻辑批量删除
   *
   * @param jobIds 作业id列表
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-05-13 16:45:42
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/delete-batch")
  public AnYiResult<String> deleteBatchByIds(
      @RequestBody @NotNullSize(message = "待删除作业id不能为空") final List<Long> jobIds) {
    service.deleteBatch(jobIds);
    return ok("批量删除成功");
  }

  /**
   * 通过作业id查询详情
   *
   * @param jobId 作业id
   * @return {@link AnYiResult }<{@link JobVo }>
   * @date 2026-05-13 16:45:42
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @Parameter(in = ParameterIn.PATH, description = "作业id", name = "jobId", required = true)
  @GetMapping(value = "/select/one/{jobId}")
  public AnYiResult<JobVo> getById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "作业id不能为空") final Long jobId) {
    return ok(service.getById(jobId));
  }

  /**
   * 通过条件查询作业信息多条数据
   *
   * @param dto 查询参数
   * @return {@link AnYiResult }<{@link List }<{@link JobVo }>>
   * @date 2026-05-13 16:45:42
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/select/list/by-model")
  public AnYiResult<List<JobVo>> selectListByModel(@RequestBody final JobQueryDto dto) {
    return ok(service.selectListByModel(dto));
  }

  /**
   * 作业信息分页查询
   *
   * @param dto 分页查询参数
   * @return {@link AnYiPageResult }<{@link JobPageVo }>
   * @date 2026-05-13 16:45:42
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/select/page")
  public AnYiResult<AnYiPageResult<JobPageVo>> selectPage(@RequestBody final JobPageDto dto) {
    return ok(service.pageByModel(dto));
  }
}
