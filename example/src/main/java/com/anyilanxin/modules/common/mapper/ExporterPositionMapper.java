package com.anyilanxin.modules.common.mapper;

import com.anyilanxin.core.AnYiBaseMapper;
import com.anyilanxin.modules.common.controller.dto.ExporterPositionPageDto;
import com.anyilanxin.modules.common.controller.dto.ExporterPositionQueryDto;
import com.anyilanxin.modules.common.entity.ExporterPositionEntity;
import com.anyilanxin.modules.common.service.vo.ExporterPositionPageVo;
import com.anyilanxin.modules.common.service.vo.ExporterPositionVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 导出记录(ExporterPosition)持久层
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 15:57:45
 * @since v1.0.0
 */
@Repository
public interface ExporterPositionMapper extends AnYiBaseMapper<ExporterPositionEntity> {
  /**
   * 分页查询
   *
   * @param dto 查询条件
   * @param page 分页信息
   * @return IPage<ExporterPositionPageVo> 查询结果
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  IPage<ExporterPositionPageVo> pageByModel(
      Page<ExporterPositionPageVo> page, @Param("query") ExporterPositionPageDto dto);

  /**
   * 条件查询多条
   *
   * @param dto 查询条件
   * @return List<ExporterPositionVo> 查询结果
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  List<ExporterPositionVo> selectListByModel(ExporterPositionQueryDto dto);
}
