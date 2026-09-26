package com.anyilanxin.modules.processinstance.service;

import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.BaseService;
import com.anyilanxin.modules.processinstance.controller.dto.ProcessInstanceCreateDto;
import com.anyilanxin.modules.processinstance.controller.dto.ProcessInstanceDto;
import com.anyilanxin.modules.processinstance.controller.dto.ProcessInstancePageDto;
import com.anyilanxin.modules.processinstance.controller.dto.ProcessInstanceQueryDto;
import com.anyilanxin.modules.processinstance.entity.ProcessInstanceEntity;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstanceBpmnBaseInfo;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstanceFlowVo;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstancePageVo;
import com.anyilanxin.modules.processinstance.service.vo.ProcessInstanceVo;
import java.util.List;

/**
 * 流程实例信息(ProcessInstance)业务层接口
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-22 15:11:53
 * @since v1.0.0
 */
public interface IProcessInstanceService extends BaseService<ProcessInstanceEntity> {
  /**
   * 保存
   *
   * @param dto 流程实例信息保存数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-22 15:11:53
   */
  void save(ProcessInstanceDto dto) throws RuntimeException;

  /**
   * 通过id更新
   *
   * @param processInstanceId 流程实例id
   * @param dto 流程实例信息更新数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-22 15:11:53
   */
  void updateById(String processInstanceId, ProcessInstanceDto dto) throws RuntimeException;

  /**
   * 分页查询
   *
   * @param dto 分页查询条件
   * @throws RuntimeException
   * @return AnYiPlusPageResult<ProcessInstancePageVo> 分页查询结果
   * @author zxh
   * @date 2026-04-22 15:11:53
   */
  AnYiPageResult<ProcessInstancePageVo> pageByModel(ProcessInstancePageDto dto)
      throws RuntimeException;

  /**
   * 条件查询多条
   *
   * @param dto 流程实例信息查询条件
   * @throws RuntimeException
   * @return List<ProcessInstanceVo> 查询结果
   * @author zxh
   * @date 2026-04-22 15:11:53
   */
  List<ProcessInstanceVo> selectListByModel(ProcessInstanceQueryDto dto) throws RuntimeException;

  /**
   * 通过id查询详情
   *
   * @param processInstanceId 流程实例id
   * @throws RuntimeException
   * @return ProcessInstanceVo 查询结果
   * @author zxh
   * @date 2026-04-22 15:11:53
   */
  ProcessInstanceVo getById(String processInstanceId) throws RuntimeException;

  /**
   * 通过processInstanceId删除
   *
   * @param processInstanceId 流程实例id
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-22 15:11:53
   */
  void deleteById(String processInstanceId) throws RuntimeException;

  /**
   * 流程实例信息批量删除
   *
   * @param processInstanceIds 流程实例id列表
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-22 15:11:53
   */
  void deleteBatch(List<String> processInstanceIds) throws RuntimeException;

  /**
   * 创建流程实例
   *
   * @param dto
   * @return {@link String }
   */
  String create(final ProcessInstanceCreateDto dto);

  /**
   * 取消流程实例
   *
   * @param processInstanceId
   * @return {@link Long }
   */
  void cancel(String processInstanceId);

  /**
   * 订阅某个流程实例的变化
   *
   * @param processInstanceId
   * @return {@link ProcessInstanceFlowVo }
   */
  ProcessInstanceFlowVo subscribeProcessInstanceChange(final String processInstanceId);

  ProcessInstanceBpmnBaseInfo queryBaseInfo(final String processInstanceId);
}
