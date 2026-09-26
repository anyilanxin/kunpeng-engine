package com.anyilanxin.modules.resource.mapper;

import com.anyilanxin.core.AnYiBaseMapper;
import com.anyilanxin.modules.resource.controller.dto.ProcessDefinitionPageDto;
import com.anyilanxin.modules.resource.controller.dto.ProcessDefinitionQueryDto;
import com.anyilanxin.modules.resource.entity.ProcessDefinitionEntity;
import com.anyilanxin.modules.resource.service.vo.ProcessDefinitionPageVo;
import com.anyilanxin.modules.resource.service.vo.ProcessDefinitionVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 部署信息(ProcessDefinition)持久层
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 16:00:12
 * @since v1.0.0
 */
@Repository
public interface ProcessDefinitionMapper extends AnYiBaseMapper<ProcessDefinitionEntity> {
  /**
   * 分页查询
   *
   * @param dto 查询条件
   * @param page 分页信息
   * @return IPage<ProcessDefinitionPageVo> 查询结果
   * @author zxh
   * @date 2026-04-23 16:00:12
   */
  IPage<ProcessDefinitionPageVo> pageByModel(
      Page<ProcessDefinitionPageVo> page, @Param("query") ProcessDefinitionPageDto dto);

  /**
   * 条件查询多条
   *
   * @param dto 查询条件
   * @return List<ProcessDefinitionVo> 查询结果
   * @author zxh
   * @date 2026-04-23 16:00:12
   */
  List<ProcessDefinitionVo> selectListByModel(ProcessDefinitionQueryDto dto);
}
