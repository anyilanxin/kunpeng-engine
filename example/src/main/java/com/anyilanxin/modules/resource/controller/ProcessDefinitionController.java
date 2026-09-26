package com.anyilanxin.modules.resource.controller;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.core.validation.annotation.NotNullSize;
import com.anyilanxin.core.validation.annotation.PathNotBlankOrNull;
import com.anyilanxin.modules.resource.controller.dto.ProcessDefinitionDto;
import com.anyilanxin.modules.resource.controller.dto.ProcessDefinitionPageDto;
import com.anyilanxin.modules.resource.controller.dto.ProcessDefinitionQueryDto;
import com.anyilanxin.modules.resource.service.IProcessDefinitionService;
import com.anyilanxin.modules.resource.service.vo.ProcessDefinitionPageVo;
import com.anyilanxin.modules.resource.service.vo.ProcessDefinitionVo;
import io.swagger.v3.oas.annotations.Hidden;
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

/**
 * 部署信息(ProcessDefinition)控制层
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 16:00:12
 * @since v1.0.0
 */
@Slf4j
@Validated
@RestController
@Hidden
@Tag(name = "ProcessDefinition", description = "部署信息相关")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@RequestMapping(value = "/processDefinition", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProcessDefinitionController extends AnYiBaseController {
  private final IProcessDefinitionService service;

  /**
   * 部署信息添加
   *
   * @param dto 待添加信息
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-04-23 16:00:12
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/insert")
  public AnYiResult<String> insert(@RequestBody @Valid final ProcessDefinitionDto dto) {
    service.save(dto);
    return ok("保存成功");
  }

  /**
   * 通过流程定义id修改
   *
   * @param processDefinitionId 流程定义id
   * @param dto 待修改信息
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-04-23 16:00:12
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @Parameter(
      in = ParameterIn.PATH,
      description = "流程定义id",
      name = "processDefinitionId",
      required = true)
  @PutMapping(value = "/update/{processDefinitionId}")
  public AnYiResult<String> update(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "流程定义id不能为空")
          final String processDefinitionId,
      @RequestBody @Valid final ProcessDefinitionDto dto) {
    service.updateById(processDefinitionId, dto);
    return ok("更新成功");
  }

  /**
   * 部署信息逻辑删除
   *
   * @param processDefinitionId 流程定义id
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-04-23 16:00:12
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @Parameter(
      in = ParameterIn.PATH,
      description = "流程定义id",
      name = "processDefinitionId",
      required = true)
  @DeleteMapping(value = "/delete-one/{processDefinitionId}")
  public AnYiResult<String> deleteById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "流程定义id不能为空")
          final String processDefinitionId) {
    service.deleteById(processDefinitionId);
    return ok("删除成功");
  }

  /**
   * 部署信息逻辑批量删除
   *
   * @param processDefinitionIds 流程定义id列表
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-04-23 16:00:12
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/delete-batch")
  public AnYiResult<String> deleteBatchByIds(
      @RequestBody @NotNullSize(message = "待删除流程定义id不能为空")
          final List<String> processDefinitionIds) {
    service.deleteBatch(processDefinitionIds);
    return ok("批量删除成功");
  }

  /**
   * 通过流程定义id查询详情
   *
   * @param processDefinitionId 流程定义id
   * @return {@link AnYiResult }<{@link ProcessDefinitionVo }>
   * @date 2026-04-23 16:00:12
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @Parameter(
      in = ParameterIn.PATH,
      description = "流程定义id",
      name = "processDefinitionId",
      required = true)
  @GetMapping(value = "/select/one/{processDefinitionId}")
  public AnYiResult<ProcessDefinitionVo> getById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "流程定义id不能为空")
          final String processDefinitionId) {
    return ok(service.getById(processDefinitionId));
  }

  /**
   * 通过条件查询部署信息多条数据
   *
   * @param dto 查询参数
   * @return {@link AnYiResult }<{@link List }<{@link ProcessDefinitionVo }>>
   * @date 2026-04-23 16:00:12
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/select/list/by-model")
  public AnYiResult<List<ProcessDefinitionVo>> selectListByModel(
      @RequestBody final ProcessDefinitionQueryDto dto) {
    return ok(service.selectListByModel(dto));
  }

  /**
   * 部署信息分页查询
   *
   * @param dto 分页查询参数
   * @return {@link AnYiPageResult }<{@link ProcessDefinitionPageVo }>
   * @date 2026-04-23 16:00:12
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/select/page")
  public AnYiResult<AnYiPageResult<ProcessDefinitionPageVo>> selectPage(
      @RequestBody final ProcessDefinitionPageDto dto) {
    return ok(service.pageByModel(dto));
  }
}
