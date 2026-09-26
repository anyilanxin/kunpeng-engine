package com.anyilanxin.modules.common.service;

import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.BaseService;
import com.anyilanxin.modules.common.controller.dto.ExporterPositionDto;
import com.anyilanxin.modules.common.controller.dto.ExporterPositionPageDto;
import com.anyilanxin.modules.common.controller.dto.ExporterPositionQueryDto;
import com.anyilanxin.modules.common.entity.ExporterPositionEntity;
import com.anyilanxin.modules.common.service.vo.ExporterPositionPageVo;
import com.anyilanxin.modules.common.service.vo.ExporterPositionVo;
import java.math.BigDecimal;
import java.util.List;

/**
 * 导出记录(ExporterPosition)业务层接口
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 15:57:45
 * @since v1.0.0
 */
public interface IExporterPositionService extends BaseService<ExporterPositionEntity> {
  /**
   * 保存
   *
   * @param dto 导出记录保存数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  void save(ExporterPositionDto dto) throws RuntimeException;

  /**
   * 通过id更新
   *
   * @param partitionId 分区id
   * @param dto 导出记录更新数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  void updateById(BigDecimal partitionId, ExporterPositionDto dto) throws RuntimeException;

  /**
   * 分页查询
   *
   * @param dto 分页查询条件
   * @throws RuntimeException
   * @return AnYiPlusPageResult<ExporterPositionPageVo> 分页查询结果
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  AnYiPageResult<ExporterPositionPageVo> pageByModel(ExporterPositionPageDto dto)
      throws RuntimeException;

  /**
   * 条件查询多条
   *
   * @param dto 导出记录查询条件
   * @throws RuntimeException
   * @return List<ExporterPositionVo> 查询结果
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  List<ExporterPositionVo> selectListByModel(ExporterPositionQueryDto dto) throws RuntimeException;

  /**
   * 通过id查询详情
   *
   * @param partitionId 分区id
   * @throws RuntimeException
   * @return ExporterPositionVo 查询结果
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  ExporterPositionVo getById(BigDecimal partitionId) throws RuntimeException;

  /**
   * 通过partitionId删除
   *
   * @param partitionId 分区id
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  void deleteById(BigDecimal partitionId) throws RuntimeException;

  /**
   * 导出记录批量删除
   *
   * @param partitionIds 分区id列表
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  void deleteBatch(List<BigDecimal> partitionIds) throws RuntimeException;
}
