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
package com.anyilanxin.modules.processinstance.controller;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.core.validation.annotation.PathNotBlankOrNull;
import com.anyilanxin.modules.processinstance.controller.dto.ProcessInstanceCreateDto;
import com.anyilanxin.modules.processinstance.controller.dto.ProcessInstancePageDto;
import com.anyilanxin.modules.processinstance.service.IProcessInstanceService;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstanceBpmnBaseInfo;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstancePageVo;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstanceVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@Tag(name = "ProcessInstance", description = "流程实例信息相关")
@MessageMapping(value = "/process-instance")
@RequestMapping(value = "/process-instance")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ProcessInstanceController extends AnYiBaseController {
  private final IProcessInstanceService service;

  @Operation(
      summary = "创建流程实例",
      tags = {"v1.0.0"},
      description = "创建流程实例")
  @PostMapping(value = "/create")
  public AnYiResult<String> create(@RequestBody @Valid final ProcessInstanceCreateDto dto) {
    return ok(service.create(dto), "创建流程实例成功");
  }

  @Operation(
      summary = "取消流程实例",
      tags = {"v1.0.0"},
      description = "取消流程实例")
  @GetMapping(value = "/cancel")
  public AnYiResult<String> cancel(
      @RequestParam(required = false) @NotNull(message = "流程实例id不能为空") final String processInstanceId) {
    service.cancel(processInstanceId);
    return ok("取消流程实例成功");
  }

  @Operation(
      summary = "通过流程实例id查询详情",
      tags = {"v1.0.0"},
      description = "查询流程实例信息详情",
      hidden = true)
  @Parameter(
      in = ParameterIn.PATH,
      description = "流程实例id",
      name = "processInstanceKey",
      required = true)
  @GetMapping(value = "/select/one/{processInstanceKey}")
  public AnYiResult<ProcessInstanceVo> getById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "流程实例id不能为空")
          final String processInstanceKey) {
    return ok(service.getById(processInstanceKey));
  }

  @Operation(
      summary = "流程实例信息分页查询",
      tags = {"v1.0.0"},
      description = "分页查询流程实例信息",
      hidden = true)
  @PostMapping(value = "/select/page")
  public AnYiResult<AnYiPageResult<ProcessInstancePageVo>> selectPage(
      @RequestBody final ProcessInstancePageDto dto) {
    return ok(service.pageByModel(dto));
  }

  /**
   * 查询流程实例基础信息 (注意前端订阅必须是/app 开头才能访问到)
   *
   * @param processInstanceId 流程实例 id
   * @return {@link AnYiResult }<{@link ProcessInstanceBpmnBaseInfo }>
   */
  @GetMapping(value = "/query-base-info/{processInstanceId}")
  public AnYiResult<ProcessInstanceBpmnBaseInfo> queryBaseInfo(
      @PathVariable final String processInstanceId) {
    return ok(service.queryBaseInfo(processInstanceId));
  }
}
