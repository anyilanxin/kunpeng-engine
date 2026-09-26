package com.anyilanxin.modules.resource.service;

import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.BaseService;
import com.anyilanxin.modules.resource.controller.dto.ProcessDefinitionDto;
import com.anyilanxin.modules.resource.controller.dto.ProcessDefinitionPageDto;
import com.anyilanxin.modules.resource.controller.dto.ProcessDefinitionQueryDto;
import com.anyilanxin.modules.resource.entity.ProcessDefinitionEntity;
import com.anyilanxin.modules.resource.service.vo.ProcessDefinitionPageVo;
import com.anyilanxin.modules.resource.service.vo.ProcessDefinitionVo;
import java.util.List;

/**
 * 部署信息(ProcessDefinition)业务层接口
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 16:00:12
 * @since v1.0.0
 */
public interface IProcessDefinitionService extends BaseService<ProcessDefinitionEntity> {
  /**
   * 保存
   *
   * @param dto 部署信息保存数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-23 16:00:12
   */
  void save(ProcessDefinitionDto dto) throws RuntimeException;

  /**
   * 通过id更新
   *
   * @param processDefinitionId 流程定义id
   * @param dto 部署信息更新数据
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-23 16:00:12
   */
  void updateById(String processDefinitionId, ProcessDefinitionDto dto) throws RuntimeException;

  /**
   * 分页查询
   *
   * @param dto 分页查询条件
   * @throws RuntimeException
   * @return AnYiPlusPageResult<ProcessDefinitionPageVo> 分页查询结果
   * @author zxh
   * @date 2026-04-23 16:00:12
   */
  AnYiPageResult<ProcessDefinitionPageVo> pageByModel(ProcessDefinitionPageDto dto)
      throws RuntimeException;

  /**
   * 条件查询多条
   *
   * @param dto 部署信息查询条件
   * @throws RuntimeException
   * @return List<ProcessDefinitionVo> 查询结果
   * @author zxh
   * @date 2026-04-23 16:00:12
   */
  List<ProcessDefinitionVo> selectListByModel(ProcessDefinitionQueryDto dto)
      throws RuntimeException;

  /**
   * 通过id查询详情
   *
   * @param processDefinitionId 流程定义id
   * @throws RuntimeException
   * @return ProcessDefinitionVo 查询结果
   * @author zxh
   * @date 2026-04-23 16:00:12
   */
  ProcessDefinitionVo getById(String processDefinitionId) throws RuntimeException;

  /**
   * 通过processDefinitionId删除
   *
   * @param processDefinitionId 流程定义id
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-23 16:00:12
   */
  void deleteById(String processDefinitionId) throws RuntimeException;

  /**
   * 部署信息批量删除
   *
   * @param processDefinitionIds 流程定义id列表
   * @throws RuntimeException
   * @author zxh
   * @date 2026-04-23 16:00:12
   */
  void deleteBatch(List<String> processDefinitionIds) throws RuntimeException;
}
