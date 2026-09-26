package com.anyilanxin.modules.common.controller;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.core.validation.annotation.NotNullSize;
import com.anyilanxin.core.validation.annotation.PathNotBlankOrNull;
import com.anyilanxin.modules.common.controller.dto.ActivityInstanceDto;
import com.anyilanxin.modules.common.controller.dto.ActivityInstancePageDto;
import com.anyilanxin.modules.common.controller.dto.ActivityInstanceQueryDto;
import com.anyilanxin.modules.common.service.IActivityInstanceService;
import com.anyilanxin.modules.common.service.vo.ActivityInstancePageVo;
import com.anyilanxin.modules.common.service.vo.ActivityInstanceVo;
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
 * 活动实例信息(ActivityInstance)控制层
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 15:57:44
 * @since v1.0.0
 */
@Slf4j
@Validated
@RestController
@Hidden
@Tag(name = "ActivityInstance", description = "活动实例信息相关")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@RequestMapping(value = "/activityInstance", produces = MediaType.APPLICATION_JSON_VALUE)
public class ActivityInstanceController extends AnYiBaseController {
  private final IActivityInstanceService service;

  /**
   * 活动实例信息添加
   *
   * @param dto 待添加信息
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-04-23 15:57:44
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/insert")
  public AnYiResult<String> insert(@RequestBody @Valid final ActivityInstanceDto dto) {
    service.save(dto);
    return ok("保存成功");
  }

  /**
   * 通过活动实例 id修改
   *
   * @param activityInstanceId 活动实例 id
   * @param dto 待修改信息
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-04-23 15:57:44
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @Parameter(
      in = ParameterIn.PATH,
      description = "活动实例 id",
      name = "activityInstanceId",
      required = true)
  @PutMapping(value = "/update/{activityInstanceId}")
  public AnYiResult<String> update(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "活动实例 id不能为空")
          final String activityInstanceId,
      @RequestBody @Valid final ActivityInstanceDto dto) {
    service.updateById(activityInstanceId, dto);
    return ok("更新成功");
  }

  /**
   * 活动实例信息逻辑删除
   *
   * @param activityInstanceId 活动实例 id
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-04-23 15:57:44
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @Parameter(
      in = ParameterIn.PATH,
      description = "活动实例 id",
      name = "activityInstanceId",
      required = true)
  @DeleteMapping(value = "/delete-one/{activityInstanceId}")
  public AnYiResult<String> deleteById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "活动实例 id不能为空")
          final String activityInstanceId) {
    service.deleteById(activityInstanceId);
    return ok("删除成功");
  }

  /**
   * 活动实例信息逻辑批量删除
   *
   * @param activityInstanceIds 活动实例 id列表
   * @return {@link AnYiResult }<{@link String }>
   * @date 2026-04-23 15:57:44
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/delete-batch")
  public AnYiResult<String> deleteBatchByIds(
      @RequestBody @NotNullSize(message = "待删除活动实例 id不能为空")
          final List<String> activityInstanceIds) {
    service.deleteBatch(activityInstanceIds);
    return ok("批量删除成功");
  }

  /**
   * 通过活动实例 id查询详情
   *
   * @param activityInstanceId 活动实例 id
   * @return {@link AnYiResult }<{@link ActivityInstanceVo }>
   * @date 2026-04-23 15:57:44
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @Parameter(
      in = ParameterIn.PATH,
      description = "活动实例 id",
      name = "activityInstanceId",
      required = true)
  @GetMapping(value = "/select/one/{activityInstanceId}")
  public AnYiResult<ActivityInstanceVo> getById(
      @PathVariable(required = false) @PathNotBlankOrNull(message = "活动实例 id不能为空")
          final String activityInstanceId) {
    return ok(service.getById(activityInstanceId));
  }

  /**
   * 通过条件查询活动实例信息多条数据
   *
   * @param dto 查询参数
   * @return {@link AnYiResult }<{@link List }<{@link ActivityInstanceVo }>>
   * @date 2026-04-23 15:57:44
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/select/list/by-model")
  public AnYiResult<List<ActivityInstanceVo>> selectListByModel(
      @RequestBody final ActivityInstanceQueryDto dto) {
    return ok(service.selectListByModel(dto));
  }

  /**
   * 活动实例信息分页查询
   *
   * @param dto 分页查询参数
   * @return {@link AnYiPageResult }<{@link ActivityInstancePageVo }>
   * @date 2026-04-23 15:57:44
   */
  @Operation(
      tags = {"v1.0.0"},
      hidden = true)
  @PostMapping(value = "/select/page")
  public AnYiResult<AnYiPageResult<ActivityInstancePageVo>> selectPage(
      @RequestBody final ActivityInstancePageDto dto) {
    return ok(service.pageByModel(dto));
  }
}
