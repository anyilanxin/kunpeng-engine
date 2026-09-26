package com.anyilanxin.modules.resource.service.impl;

import static com.anyilanxin.core.BaseService.getPage;
import static com.anyilanxin.core.BaseService.toPageData;

import com.anyilanxin.core.AnYiPageResult;
import com.anyilanxin.core.AnYiResultStatus;
import com.anyilanxin.core.exception.AnYiResponseException;
import com.anyilanxin.modules.resource.controller.dto.ProcessDefinitionDto;
import com.anyilanxin.modules.resource.controller.dto.ProcessDefinitionPageDto;
import com.anyilanxin.modules.resource.controller.dto.ProcessDefinitionQueryDto;
import com.anyilanxin.modules.resource.entity.ProcessDefinitionEntity;
import com.anyilanxin.modules.resource.mapper.ProcessDefinitionMapper;
import com.anyilanxin.modules.resource.service.IProcessDefinitionService;
import com.anyilanxin.modules.resource.service.vo.ProcessDefinitionPageVo;
import com.anyilanxin.modules.resource.service.vo.ProcessDefinitionVo;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.linpeilie.Converter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 部署信息(ProcessDefinition)业务层实现
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 16:00:12
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ProcessDefinitionServiceImpl
    extends ServiceImpl<ProcessDefinitionMapper, ProcessDefinitionEntity>
    implements IProcessDefinitionService {
  private final Converter converter;
  private final ProcessDefinitionMapper mapper;

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void save(final ProcessDefinitionDto dto) throws RuntimeException {
    final var entity = converter.convert(dto, ProcessDefinitionEntity.class);
    final var result = super.save(entity);
    if (!result) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "保存数据失败");
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void updateById(final String processDefinitionId, final ProcessDefinitionDto dto)
      throws RuntimeException {
    // 查询数据是否存在
    getById(processDefinitionId);
    // 更新数据
    final var entity = converter.convert(dto, ProcessDefinitionEntity.class);
    entity.setProcessDefinitionId(processDefinitionId);
    final var result = super.updateById(entity);
    if (!result) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "更新数据失败");
    }
  }

  @Override
  public List<ProcessDefinitionVo> selectListByModel(final ProcessDefinitionQueryDto dto)
      throws RuntimeException {
    final var list = mapper.selectListByModel(dto);
    if (list == null || list.isEmpty()) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "未找到符合条件数据");
    }
    return list;
  }

  @Override
  public AnYiPageResult<ProcessDefinitionPageVo> pageByModel(final ProcessDefinitionPageDto dto)
      throws RuntimeException {
    return toPageData(mapper.pageByModel(getPage(dto), dto));
  }

  @Override
  public ProcessDefinitionVo getById(final String processDefinitionId) throws RuntimeException {
    final var byId = super.getById(processDefinitionId);
    if (Objects.isNull(byId)) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "未找到符合条件数据");
    }
    return converter.convert(byId, ProcessDefinitionVo.class);
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void deleteById(final String processDefinitionId) throws RuntimeException {
    // 查询数据是否存在
    getById(processDefinitionId);
    // 删除数据
    final var b = removeById(processDefinitionId);
    if (!b) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "删除数据失败");
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, Error.class})
  public void deleteBatch(final List<String> processDefinitionIds) throws RuntimeException {
    final var entities = listByIds(processDefinitionIds);
    if (entities == null || entities.isEmpty()) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "数据不存在或已经被别人删除");
    }
    final var waitDeleteList = new ArrayList<String>();
    entities.forEach(v -> waitDeleteList.add(v.getProcessDefinitionId()));
    final var i = mapper.deleteByIds(waitDeleteList);
    if (i <= 0) {
      throw new AnYiResponseException(AnYiResultStatus.DATABASE_BASE_ERROR, "批量删除成功");
    }
  }
}
