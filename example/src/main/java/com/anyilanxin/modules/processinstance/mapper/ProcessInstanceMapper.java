package com.anyilanxin.modules.processinstance.mapper;

import com.anyilanxin.core.AnYiBaseMapper;
import com.anyilanxin.modules.processinstance.controller.dto.ProcessInstancePageDto;
import com.anyilanxin.modules.processinstance.controller.dto.ProcessInstanceQueryDto;
import com.anyilanxin.modules.processinstance.entity.ProcessInstanceEntity;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstancePageVo;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstanceVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 流程实例信息(ProcessInstance)持久层
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-22 15:11:53
 * @since v1.0.0
 */
@Repository
public interface ProcessInstanceMapper extends AnYiBaseMapper<ProcessInstanceEntity> {
  /**
   * 分页查询
   *
   * @param dto 查询条件
   * @param page 分页信息
   * @return IPage<ProcessInstancePageVo> 查询结果
   * @author zxh
   * @date 2026-04-22 15:11:53
   */
  IPage<ProcessInstancePageVo> pageByModel(
      Page<ProcessInstancePageVo> page, @Param("query") ProcessInstancePageDto dto);

  /**
   * 条件查询多条
   *
   * @param dto 查询条件
   * @return List<ProcessInstanceVo> 查询结果
   * @author zxh
   * @date 2026-04-22 15:11:53
   */
  List<ProcessInstanceVo> selectListByModel(ProcessInstanceQueryDto dto);
}
