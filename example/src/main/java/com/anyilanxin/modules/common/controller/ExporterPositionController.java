package com.anyilanxin.modules.common.controller;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.core.validation.annotation.NotNullSize;
import com.anyilanxin.core.validation.annotation.PathNotBlankOrNull;
import com.anyilanxin.modules.common.controller.dto.ExporterPositionDto;
import com.anyilanxin.modules.common.controller.dto.ExporterPositionPageDto;
import com.anyilanxin.modules.common.controller.dto.ExporterPositionQueryDto;
import com.anyilanxin.modules.common.service.IExporterPositionService;
import com.anyilanxin.modules.common.service.vo.ExporterPositionPageVo;
import com.anyilanxin.modules.common.service.vo.ExporterPositionVo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 导出记录(ExporterPosition)控制层
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 15:57:45
 * @since v1.0.0
 */
@Slf4j
@Validated
@RestController
@Hidden
@Tag(name = "ExporterPosition", description = "导出记录相关")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@RequestMapping(value = "/exporterPosition", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExporterPositionController extends AnYiBaseController {
  private final IExporterPositionService service;

  /**
   * 导出记录添加
   *
   * @param dto 待添加信息
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-04-23 15:57:45
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/insert")
  public AnYiResult<String> insert(@RequestBody @Valid ExporterPositionDto dto) {
    service.save(dto);
    return ok("保存成功");
  }

  /**
   * 通过分区id修改
   *
   * @param partitionId 分区id
   * @param dto 待修改信息
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-04-23 15:57:45
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @Parameter(in = ParameterIn.PATH, description = "分区id", name = "partitionId", required = true)
  @PutMapping(value = "/update/{partitionId}")
  public AnYiResult<String> update(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "分区id不能为空")
          BigDecimal partitionId,
      @RequestBody @Valid ExporterPositionDto dto) {
    service.updateById(partitionId, dto);
    return ok("更新成功");
  }

  /**
   * 导出记录逻辑删除
   *
   * @param partitionId 分区id
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-04-23 15:57:45
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @Parameter(in = ParameterIn.PATH, description = "分区id", name = "partitionId", required = true)
  @DeleteMapping(value = "/delete-one/{partitionId}")
  public AnYiResult<String> deleteById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "分区id不能为空")
          BigDecimal partitionId) {
    service.deleteById(partitionId);
    return ok("删除成功");
  }

  /**
   * 导出记录逻辑批量删除
   *
   * @param partitionIds 分区id列表
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-04-23 15:57:45
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/delete-batch")
  public AnYiResult<String> deleteBatchByIds(
      @RequestBody @NotNullSize(message = "待删除分区id不能为空") List<BigDecimal> partitionIds) {
    service.deleteBatch(partitionIds);
    return ok("批量删除成功");
  }

  /**
   * 通过分区id查询详情
   *
   * @param partitionId 分区id
   * @return {@link AnYiResult }<{@link ExporterPositionVo }>
   * @date 2026-04-23 15:57:45
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @Parameter(in = ParameterIn.PATH, description = "分区id", name = "partitionId", required = true)
  @GetMapping(value = "/select/one/{partitionId}")
  public AnYiResult<ExporterPositionVo> getById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "分区id不能为空")
          BigDecimal partitionId) {
    return ok(service.getById(partitionId));
  }

  /**
   * 通过条件查询导出记录多条数据
   *
   * @param dto 查询参数
   * @return {@link AnYiResult }<{@link List }<{@link ExporterPositionVo }>>
   * @date 2026-04-23 15:57:45
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/select/list/by-model")
  public AnYiResult<List<ExporterPositionVo>> selectListByModel(
      @RequestBody ExporterPositionQueryDto dto) {
    return ok(service.selectListByModel(dto));
  }

  /**
   * 导出记录分页查询
   *
   * @param dto 分页查询参数
   * @return {@link AnYiPageResult }<{@link ExporterPositionPageVo }>
   * @date 2026-04-23 15:57:45
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/select/page")
  public AnYiResult<AnYiPageResult<ExporterPositionPageVo>> selectPage(
      @RequestBody ExporterPositionPageDto dto) {
    return ok(service.pageByModel(dto));
  }
}
