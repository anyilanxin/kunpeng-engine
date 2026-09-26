package com.anyilanxin.modules.common.mapper;

import com.anyilanxin.core.AnYiBaseMapper;
import com.anyilanxin.modules.common.controller.dto.ActivityInstancePageDto;
import com.anyilanxin.modules.common.controller.dto.ActivityInstanceQueryDto;
import com.anyilanxin.modules.common.entity.ActivityInstanceEntity;
import com.anyilanxin.modules.common.service.vo.ActivityInstancePageVo;
import com.anyilanxin.modules.common.service.vo.ActivityInstanceVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 活动实例信息(ActivityInstance)持久层
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 15:57:45
 * @since v1.0.0
 */
@Repository
public interface ActivityInstanceMapper extends AnYiBaseMapper<ActivityInstanceEntity> {
  /**
   * 分页查询
   *
   * @param dto 查询条件
   * @param page 分页信息
   * @return IPage<ActivityInstancePageVo> 查询结果
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  IPage<ActivityInstancePageVo> pageByModel(
      Page<ActivityInstancePageVo> page, @Param("query") ActivityInstancePageDto dto);

  /**
   * 条件查询多条
   *
   * @param dto 查询条件
   * @return List<ActivityInstanceVo> 查询结果
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  List<ActivityInstanceVo> selectListByModel(ActivityInstanceQueryDto dto);
}
