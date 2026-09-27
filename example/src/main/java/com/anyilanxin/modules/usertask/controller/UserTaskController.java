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
package com.anyilanxin.modules.usertask.controller;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.core.validation.annotation.NotNullSize;
import com.anyilanxin.core.validation.annotation.PathNotBlankOrNull;
import com.anyilanxin.modules.usertask.controller.dto.*;
import com.anyilanxin.modules.usertask.service.IUserTaskService;
import com.anyilanxin.modules.usertask.service.vo.UserTaskPageVo;
import com.anyilanxin.modules.usertask.service.vo.UserTaskVo;
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
@Tag(name = "UserTask", description = "用户任务信息相关")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@RequestMapping(value = "/user-task", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserTaskController extends AnYiBaseController {
  private final IUserTaskService service;

  @Operation(
      summary = "完成用户任务",
      tags = {"v1.0.0"},
      description = "完成用户任务")
  @PostMapping(value = "/complete")
  public AnYiResult<String> complete(@RequestBody final UserTaskCompleteDto dto) {
    service.complete(dto);
    return ok("完成用户成功");
  }

  @Operation(
      summary = "取消用户任务",
      tags = {"v1.0.0"},
      description = "取消用户任务")
  @PostMapping(value = "/cancel")
  public AnYiResult<String> cancel(@RequestBody final UserTaskCancelDto dto) {
    service.cancel(dto);
    return ok("取消用户任务成功");
  }

  @Operation(
      summary = "用户任务信息添加",
      tags = {"v1.0.0"},
      description = "添加用户任务信息",
      hidden = true)
  @PostMapping(value = "/insert")
  public AnYiResult<String> insert(@RequestBody @Valid final UserTaskDto dto) {
    service.save(dto);
    return ok("保存成功");
  }

  @Operation(
      summary = "通过用户任务 key修改",
      tags = {"v1.0.0"},
      description = "修改用户任务信息",
      hidden = true)
  @Parameter(in = ParameterIn.PATH, description = "用户任务 key", name = "userTaskKey", required = true)
  @PutMapping(value = "/update/{userTaskKey}")
  public AnYiResult<String> update(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "用户任务 key不能为空")
          final String userTaskKey,
      @RequestBody @Valid final UserTaskDto dto) {
    service.updateById(userTaskKey, dto);
    return ok("更新成功");
  }

  @Operation(
      summary = "用户任务信息逻辑删除",
      tags = {"v1.0.0"},
      description = "删除用户任务信息",
      hidden = true)
  @Parameter(in = ParameterIn.PATH, description = "用户任务 key", name = "userTaskKey", required = true)
  @DeleteMapping(value = "/delete-one/{userTaskKey}")
  public AnYiResult<String> deleteById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "用户任务 key不能为空")
          final String userTaskKey) {
    service.deleteById(userTaskKey);
    return ok("删除成功");
  }

  @Operation(
      summary = "用户任务信息逻辑批量删除",
      tags = {"v1.0.0"},
      description = "批量删除用户任务信息",
      hidden = true)
  @PostMapping(value = "/delete-batch")
  public AnYiResult<String> deleteBatchByIds(
      @RequestBody @NotNullSize(message = "待删除用户任务 key不能为空") final List<String> userTaskKeys) {
    service.deleteBatch(userTaskKeys);
    return ok("批量删除成功");
  }

  @Operation(
      summary = "通过用户任务 key查询详情",
      tags = {"v1.0.0"},
      description = "查询用户任务信息详情",
      hidden = true)
  @Parameter(in = ParameterIn.PATH, description = "用户任务 key", name = "userTaskKey", required = true)
  @GetMapping(value = "/select/one/{userTaskKey}")
  public AnYiResult<UserTaskVo> getById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "用户任务 key不能为空")
          final String userTaskKey) {
    return ok(service.getById(userTaskKey));
  }

  @Operation(
      summary = "通过条件查询用户任务信息多条数据",
      tags = {"v1.0.0"},
      description = "通过条件查询用户任务信息",
      hidden = true)
  @PostMapping(value = "/select/list/by-model")
  public AnYiResult<List<UserTaskVo>> selectListByModel(@RequestBody final UserTaskQueryDto dto) {
    return ok(service.selectListByModel(dto));
  }

  @Operation(
      summary = "用户任务信息分页查询",
      tags = {"v1.0.0"},
      description = "分页查询用户任务信息",
      hidden = true)
  @PostMapping(value = "/select/page")
  public AnYiResult<AnYiPageResult<UserTaskPageVo>> selectPage(
      @RequestBody final UserTaskPageDto dto) {
    return ok(service.pageByModel(dto));
  }
}
