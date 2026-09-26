package com.anyilanxin.modules.common.service;

import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.BaseService;
import com.anyilanxin.modules.common.controller.dto.ActivityInstanceDto;
import com.anyilanxin.modules.common.controller.dto.ActivityInstancePageDto;
import com.anyilanxin.modules.common.controller.dto.ActivityInstanceQueryDto;
import com.anyilanxin.modules.common.entity.ActivityInstanceEntity;
import com.anyilanxin.modules.common.service.vo.ActivityInstancePageVo;
import com.anyilanxin.modules.common.service.vo.ActivityInstanceVo;
import java.util.List;

/**
 * 活动实例信息(ActivityInstance)业务层接口
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 15:57:45
 * @since v1.0.0
 */
public interface IActivityInstanceService extends BaseService<ActivityInstanceEntity> {
  /**
   * 保存
   *
   * @param dto 活动实例信息保存数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  void save(ActivityInstanceDto dto) throws RuntimeException;

  /**
   * 通过id更新
   *
   * @param activityInstanceId 活动实例 id
   * @param dto 活动实例信息更新数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  void updateById(String activityInstanceId, ActivityInstanceDto dto) throws RuntimeException;

  /**
   * 分页查询
   *
   * @param dto 分页查询条件
   * @throws RuntimeException
   * @return AnYiPlusPageResult<ActivityInstancePageVo> 分页查询结果
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  AnYiPageResult<ActivityInstancePageVo> pageByModel(ActivityInstancePageDto dto)
      throws RuntimeException;

  /**
   * 条件查询多条
   *
   * @param dto 活动实例信息查询条件
   * @throws RuntimeException
   * @return List<ActivityInstanceVo> 查询结果
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  List<ActivityInstanceVo> selectListByModel(ActivityInstanceQueryDto dto) throws RuntimeException;

  /**
   * 通过id查询详情
   *
   * @param activityInstanceId 活动实例 id
   * @throws RuntimeException
   * @return ActivityInstanceVo 查询结果
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  ActivityInstanceVo getById(String activityInstanceId) throws RuntimeException;

  /**
   * 通过activityInstanceId删除
   *
   * @param activityInstanceId 活动实例 id
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  void deleteById(String activityInstanceId) throws RuntimeException;

  /**
   * 活动实例信息批量删除
   *
   * @param activityInstanceIds 活动实例 id列表
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-23 15:57:45
   */
  void deleteBatch(List<String> activityInstanceIds) throws RuntimeException;
}
